package io.scraperworker.scraper;


import io.scraperworker.config.ScraperProperties;
import io.scraperworker.kafka.events.ScrapeRequestedEvent;
import io.scraperworker.kafka.producer.ScrapeEventProducer;
import io.scraperworker.model.Job;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndeedScraper {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ScraperProperties properties;
    private final ScrapeEventProducer eventProducer;
    private final IndeedUrlBuilder urlBuilder;

    private static final String INDEED_DOMAIN = "https://www.indeed.com";
    private static final String JOB_DETAIL_URL = "https://www.indeed.com/viewjob?jk=";

    private static final Pattern MOSAIC_DATA_PATTERN = Pattern.compile(
            "window\\.mosaic\\.providerData\\[\"mosaic-provider-jobcards\"\\]\\s*=\\s*(\\{.+?\\});",
            Pattern.DOTALL
    );
    private static final Pattern INITIAL_DATA_PATTERN = Pattern.compile(
            "_initialData\\s*=\\s*(\\{.+?\\});",
            Pattern.DOTALL
    );

    public void scrape(ScrapeRequestedEvent event) {
        UUID taskId = event.getTaskId();
        UUID userId = event.getUserId();
        int jobCount = event.getNumJobs();

        log.info("[TaskID: {}] Starting scrape. JobTitle: {}, Location: {}, NumJobs: {}",
                taskId, event.getJobTitle(), event.getLocation(), jobCount);

        String initialUrl = event.getSearchUrl() != null
                ? event.getSearchUrl()
                : urlBuilder.buildSearchUrl(event.getJobTitle(), event.getLocation(), event.getAge());

        String currentUrl = urlBuilder.cleanUrl(initialUrl);
        int currentPage = urlBuilder.extractStartPage(currentUrl);
        String sessionId = generateSessionId(taskId.toString());

        try {
            // ── Phase 1: Collect all job stubs from search result pages ──
            List<JobStub> allStubs = new ArrayList<>();

            while (allStubs.size() < jobCount) {
                String targetUrl = urlBuilder.buildUrlWithPage(currentUrl, currentPage);
                log.info("[+] Current URL {}", targetUrl);
                Optional<PageData> pageDataOptional = fetchPageWithRetry(targetUrl, sessionId, taskId.toString());

                if (pageDataOptional.isEmpty()) {
                    log.warn("[TaskID: {}] Failed to fetch page after retries", taskId);
                    break;
                }

                PageData pageData = pageDataOptional.get();
                List<JobStub> jobStubs = extractJobStubsFromPage(pageData.getResultsNode());

                int needed = jobCount - allStubs.size();
                List<JobStub> stubsToTake = jobStubs.stream().limit(needed).toList();
                allStubs.addAll(stubsToTake);

                log.info("[TaskID: {}] Collected {} stubs from page {}. Total stubs: {}/{}",
                        taskId, stubsToTake.size(), (currentPage / 10) + 1, allStubs.size(), jobCount);

                if (!pageData.hasNextPage || allStubs.size() >= jobCount) {
                    break;
                }

                currentPage += 10;
                sleepBetweenPages();
            }

            log.info("[TaskID: {}] Phase 1 complete. Collected {} stubs total.", taskId, allStubs.size());

            if (allStubs.isEmpty()) {
                eventProducer.publishScrapeFailed(taskId, userId, "No jobs found");
                return;
            }

            // ── Phase 2: Fetch full job details for each stub ──
            List<Job> collectedJobs = new ArrayList<>();
            String detailSessionId = generateSessionId(taskId.toString());

            for (int i = 0; i < allStubs.size(); i++) {
                JobStub stub = allStubs.get(i);
                log.info("[TaskID: {}] Fetching job details [{}/{}] for jobKey: {}",
                        taskId, i + 1, allStubs.size(), stub.getJobKey());

                Optional<Job> fullJob = fetchJobDetailsWithRetry(stub, detailSessionId, taskId.toString());
                if (fullJob.isPresent()) {
                    fullJob.get().setDescription(Jsoup.parse(fullJob.get().getDescription()).text());
                    collectedJobs.add(fullJob.get());
                } else {
                    // Fallback to stub data if detail fetch fails
                    collectedJobs.add(stub.toJob());
                }
                sleepBetweenRequests();
            }

            log.info("[TaskID: {}] Scrape complete. Total jobs with details: {}", taskId, collectedJobs.size());
            eventProducer.publishScrapeCompleted(event, collectedJobs);

        } catch (Exception e) {
            log.error("[TaskID: {}] Unexpected error during scrape: {}", taskId, e.getMessage(), e);
            eventProducer.publishScrapeFailed(taskId, userId, "Scrape failed: " + e.getMessage());
        }
    }

    private Optional<Job> fetchJobDetailsWithRetry(JobStub stub, String sessionId, String taskId) {
        int maxAttempts = properties.getScraper().getMaxAttempts();
        String currentSessionId = sessionId;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Optional<Job> result = fetchJobDetails(stub, currentSessionId, taskId);
            if (result.isPresent()) {
                return result;
            }

            log.warn("[TaskID: {}] Job detail attempt {}/{} failed for jobKey: {}, retrying...",
                    taskId, attempt + 1, maxAttempts, stub.getJobKey());
            currentSessionId = generateSessionId(taskId);

            try {
                Thread.sleep(3000L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.warn("[TaskID: {}] All attempts exhausted for jobKey: {}, falling back to stub", taskId, stub.getJobKey());
        return Optional.empty();
    }

    private Optional<Job> fetchJobDetails(JobStub stub, String sessionId, String taskId) {
        String detailUrl = JOB_DETAIL_URL + stub.getJobKey();
        String proxyUrl = buildScrapflyProxyUrl(detailUrl, sessionId);

        log.debug("[TaskID: {}] Fetching job details for jobKey: {}", taskId, stub.getJobKey());

        try {
            String rawResponse = restTemplate.getForObject(proxyUrl, String.class);
            if (rawResponse == null) {
                log.warn("[TaskID: {}] Empty response for job detail: {}", taskId, stub.getJobKey());
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(rawResponse);

            if (!rootNode.hasNonNull("result") || !rootNode.get("result").hasNonNull("content")) {
                log.warn("[TaskID: {}] Missing content in job detail response", taskId);
                return Optional.empty();
            }

            // Check for login redirect on detail pages too
            if (isLoginRedirect(rootNode)) {
                log.warn("[TaskID: {}] Login redirect detected on job detail: {}", taskId, stub.getJobKey());
                return Optional.empty();
            }

            String htmlContent = rootNode.path("result").path("content").asText();
            Optional<String> jsonDataOpt = extractInitialData(htmlContent);

            if (jsonDataOpt.isEmpty()) {
                log.warn("[TaskID: {}] Could not extract _initialData for jobKey: {}", taskId, stub.getJobKey());
                return Optional.empty();
            }

            JsonNode initialData = objectMapper.readTree(jsonDataOpt.get());
            JsonNode jobInfo = initialData.path("jobInfoWrapperModel").path("jobInfoModel");

            String description = jobInfo.path("sanitizedJobDescription").asText(null);
            if (description == null) {
                description = jobInfo.path("jobDescription").asText(null);
            }

            return Optional.of(Job.builder()
                    .jobTitle(stub.getJobTitle())
                    .companyName(stub.getCompanyName())
                    .location(stub.getLocation())
                    .datePosted(stub.getDatePosted())
                    .salary(stub.getSalary())
                    .jobUrl(stub.getJobUrl())
                    .description(description)
                    .build());

        } catch (Exception e) {
            log.error("[TaskID: {}] Error fetching job details for {}: {}", taskId, stub.getJobKey(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> extractInitialData(String htmlContent) {
        Matcher matcher = INITIAL_DATA_PATTERN.matcher(htmlContent);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<PageData> fetchPageWithRetry(String targetUrl, String sessionId, String taskId) {
        int maxAttempts = properties.getScraper().getMaxAttempts();
        String currentSessionId = sessionId;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String proxyUrl = buildScrapflyProxyUrl(targetUrl, currentSessionId);

            log.info("[TaskID: {}] Attempt {}/{} for URL: {}", taskId, attempt + 1, maxAttempts, targetUrl);

            Optional<PageData> result = fetchAndParsePage(proxyUrl, targetUrl, taskId);
            if (result.isPresent()) {
                return result;
            }

            log.warn("[TaskID: {}] Attempt {} failed, retrying...", taskId, attempt + 1);
            currentSessionId = generateSessionId(taskId);

            try {
                Thread.sleep(4000L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return Optional.empty();
    }

    private Optional<PageData> fetchAndParsePage(String proxyUrl, String targetUrl, String taskId) {
        try {
            String rawResponse = restTemplate.getForObject(proxyUrl, String.class);
            if (rawResponse == null) {
                log.error("[TaskID: {}] Empty response from Scrapfly", taskId);
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(rawResponse);

            if (rootNode.hasNonNull("result") && rootNode.get("result").hasNonNull("error")) {
                log.error("[TaskID: {}] Scrapfly error: {}", taskId, rootNode.get("result").get("error").asText());
                return Optional.empty();
            }

            if (!rootNode.hasNonNull("result") || !rootNode.get("result").hasNonNull("content")) {
                log.error("[TaskID: {}] Missing result.content in response", taskId);
                return Optional.empty();
            }

            String htmlContent = rootNode.path("result").path("content").asText();
            if (htmlContent.isEmpty()) {
                log.warn("[TaskID: {}] Empty HTML content", taskId);
                return Optional.empty();
            }

            if (isLoginRedirect(rootNode)) {
                log.warn("[TaskID: {}] Login redirect detected", taskId);
                return Optional.empty();
            }

            Document htmlDoc = Jsoup.parse(htmlContent);
            Element scriptElement = htmlDoc.getElementById("mosaic-data");

            if (scriptElement == null) {
                log.warn("[TaskID: {}] Could not find mosaic-data element", taskId);
                return Optional.empty();
            }

            Optional<String> jsonDataOpt = extractJsonDataFromScript(scriptElement.html());
            if (jsonDataOpt.isEmpty()) {
                log.warn("[TaskID: {}] Could not extract JSON from mosaic script", taskId);
                return Optional.empty();
            }

            JsonNode mosaicDataRoot = objectMapper.readTree(jsonDataOpt.get());
            JsonNode resultsNode = mosaicDataRoot.path("metaData").path("mosaicProviderJobCardsModel").path("results");

            boolean hasNextPage = htmlDoc.selectFirst("a[aria-label=Next Page], button[aria-label=Next Page]") != null;

            return Optional.of(new PageData(resultsNode, hasNextPage));

        } catch (Exception e) {
            log.error("[TaskID: {}] Error fetching/parsing page: {}", taskId, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isLoginRedirect(JsonNode rootNode) {
        String finalUrl = rootNode.path("result").path("url").asText("");
        if (finalUrl.isEmpty()) return false;

        String lowerUrl = finalUrl.toLowerCase();
        return lowerUrl.contains("secure.indeed.com") ||
                lowerUrl.contains("/account/login") ||
                lowerUrl.contains("/auth/") ||
                lowerUrl.contains("signin") ||
                lowerUrl.contains("/login");
    }

    private List<JobStub> extractJobStubsFromPage(JsonNode resultsNode) {
        if (resultsNode == null || !resultsNode.isArray() || resultsNode.isEmpty()) {
            return Collections.emptyList();
        }

        List<JobStub> stubs = new ArrayList<>();
        for (JsonNode jobNode : resultsNode) {
            try {
                if (!(jobNode.hasNonNull("adBlob") || jobNode.hasNonNull("adId") || jobNode.hasNonNull("advn"))) {
                    stubs.add(extractJobStub(jobNode));
                }
            } catch (Exception e) {
                log.error("Failed to extract job stub: {}", e.getMessage());
            }
        }
        return stubs;
    }

    private JobStub extractJobStub(JsonNode jobNode) {
        String jobKey = getTextOrNull(jobNode, "jobkey");
        return JobStub.builder()
                .jobKey(jobKey)
                .jobTitle(getTextOrNull(jobNode, "displayTitle"))
                .companyName(getTextOrNull(jobNode, "company"))
                .location(getTextOrNull(jobNode, "formattedLocation"))
                .datePosted(getDateOrNull(jobNode, "createDate"))
                .salary(jobNode.path("salarySnippet").path("text").asText(null))
                .jobUrl(INDEED_DOMAIN + "/viewjob?jk=" + (jobKey != null ? jobKey : ""))
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Instant getDateOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        long timestamp = value.asLong(0);
        return timestamp > 0 ? Instant.ofEpochMilli(timestamp) : null;
    }

    private Optional<String> extractJsonDataFromScript(String scriptContent) {
        if (scriptContent == null || scriptContent.isEmpty()) {
            return Optional.empty();
        }
        Matcher matcher = MOSAIC_DATA_PATTERN.matcher(scriptContent);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private String buildScrapflyProxyUrl(String targetUrl, String sessionId) {
        String decodedUrl;
        try {
            decodedUrl = URLDecoder.decode(targetUrl, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decodedUrl = targetUrl;
        }
        String encodedTargetUrl = URLEncoder.encode(decodedUrl, StandardCharsets.UTF_8);
        return properties.getScrapfly().getBaseUrl() +
                "?key=" + properties.getScrapfly().getApiKey() +
                "&url=" + encodedTargetUrl +
                "&session=" + sessionId +
                "&proxy_pool=public_residential_pool" +
                "&session_sticky_proxy=true" +
                "&render_js=true" +
                "&rendering_wait=4000" +
                "&auto_scroll=true" +
                "&asp=true" +
                "&country=us" +
                "&retry=true";
    }

    private String generateSessionId(String taskId) {
        return taskId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void sleepBetweenPages() {
        try {
            int delay = properties.getScraper().getPageDelayMs() +
                    new Random().nextInt(properties.getScraper().getPageDelayRandomMs());
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleepBetweenRequests() {
        try {
            Thread.sleep(500 + new Random().nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class PageData {
        private final JsonNode resultsNode;
        private final boolean hasNextPage;
    }

    @Builder
    @Getter
    private static class JobStub {
        private final String jobKey;
        private final String jobTitle;
        private final String companyName;
        private final String location;
        private final Instant datePosted;
        private final String salary;
        private final String jobUrl;

        public Job toJob() {
            return Job.builder()
                    .jobTitle(jobTitle)
                    .companyName(companyName)
                    .location(location)
                    .datePosted(datePosted)
                    .salary(salary)
                    .jobUrl(jobUrl)
                    .description(null)
                    .build();
        }
    }
}