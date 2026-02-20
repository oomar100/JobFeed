package io.match.matchservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.match.matchservice.config.RankerProperties;
import io.match.matchservice.dto.GeminiJobRequest;
import io.match.matchservice.dto.GeminiJobResponse;
import io.match.matchservice.dto.ScrapedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RankerProperties properties;

    private static final int BATCH_SIZE = 10;

    /**
     * Ranks jobs in batches of 10.
     * Returns a map of job index -> score
     */
    public Map<Integer, Integer> rankJobs(List<ScrapedJob> jobs, List<String> skills,
                                          Integer yearsOfExperience, List<String> thingsToAvoid,
                                          String additionalPreferences) {

        Map<Integer, Integer> scores = new HashMap<>();
        List<List<ScrapedJob>> batches = partitionIntoBatches(jobs, BATCH_SIZE);

        int globalIndex = 0;
        for (int batchNum = 0; batchNum < batches.size(); batchNum++) {
            List<ScrapedJob> batch = batches.get(batchNum);
            log.info("Processing batch {}/{} with {} jobs", batchNum + 1, batches.size(), batch.size());

            try {
                Map<Integer, Integer> batchScores = rankBatch(
                        batch, globalIndex, skills, yearsOfExperience, thingsToAvoid, additionalPreferences
                );
                scores.putAll(batchScores);
            } catch (Exception e) {
                log.error("Error ranking batch {}: {}", batchNum + 1, e.getMessage());
                // Default score for failed batch
                for (int i = 0; i < batch.size(); i++) {
                    scores.put(globalIndex + i, 5);
                }
            }

            globalIndex += batch.size();
        }

        return scores;
    }

    private Map<Integer, Integer> rankBatch(List<ScrapedJob> batch, int startIndex,
                                            List<String> skills, Integer yearsOfExperience,
                                            List<String> thingsToAvoid, String additionalPreferences) {

        GeminiJobRequest request = buildRequest(batch, startIndex, skills, yearsOfExperience, thingsToAvoid, additionalPreferences);
        String prompt = buildPrompt(request);
        String response = callGemini(prompt);
        return parseResponse(response, batch.size(), startIndex);
    }

    private GeminiJobRequest buildRequest(List<ScrapedJob> batch, int startIndex,
                                          List<String> skills, Integer yearsOfExperience,
                                          List<String> thingsToAvoid, String additionalPreferences) {

        List<GeminiJobRequest.JobToRank> jobsToRank = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            ScrapedJob job = batch.get(i);
            jobsToRank.add(GeminiJobRequest.JobToRank.builder()
                    .id(startIndex + i)
                    .jobTitle(job.getJobTitle())
                    .companyName(job.getCompanyName())
                    .location(job.getLocation())
                    .salary(job.getSalary())
                    .description(truncateDescription(job.getDescription()))
                    .build());
        }

        return GeminiJobRequest.builder()
                .preferences(GeminiJobRequest.Preferences.builder()
                        .skills(skills)
                        .yearsOfExperience(yearsOfExperience)
                        .thingsToAvoid(thingsToAvoid)
                        .additionalPreferences(additionalPreferences)
                        .build())
                .jobs(jobsToRank)
                .build();
    }

    private String buildPrompt(GeminiJobRequest request) {
        try {
            String jobsJson = objectMapper.writeValueAsString(request);

            return """
                You are a job matching assistant. Score each job from 1-10 based on how well it matches the candidate's preferences.
                1 = No match, 10 = Perfect match.
                
                IMPORTANT RULES:
                - If "thingsToAvoid" keywords appear in the job description, lower the score significantly (1-3)
                - Match skills mentioned in the job with candidate's skills
                - Consider years of experience requirements
                - Consider additional preferences
                
                INPUT:
                %s
                
                RESPOND WITH ONLY valid JSON in this exact format, no other text:
                {"jobs":[{"id":0,"score":7},{"id":1,"score":5},...]}
                """.formatted(jobsJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request", e);
        }
    }

    private String callGemini(String prompt) {
        String url = String.format("%s/%s:generateContent?key=%s",
                properties.getGemini().getBaseUrl(),
                properties.getGemini().getModel(),
                properties.getGemini().getApiKey());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(Map.of("text", prompt)));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(url, request, String.class);
    }

    private Map<Integer, Integer> parseResponse(String response, int batchSize, int startIndex) {
        Map<Integer, Integer> scores = new HashMap<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            String text = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText()
                    .trim();

            // Clean up response - remove markdown code blocks if present
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            GeminiJobResponse geminiResponse = objectMapper.readValue(text, GeminiJobResponse.class);

            for (GeminiJobResponse.JobScore jobScore : geminiResponse.getJobs()) {
                int score = Math.max(1, Math.min(10, jobScore.getScore()));
                scores.put(jobScore.getId(), score);
            }

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            // Default scores on parse failure
            for (int i = 0; i < batchSize; i++) {
                scores.put(startIndex + i, -1);
            }
        }

        return scores;
    }

    private String truncateDescription(String description) {
        if (description == null) return null;
        // Truncate to ~1000 chars to stay within token limits
        return description.length() > 1000 ? description.substring(0, Math.min(description.length(), 8000)) + "..." : description;
    }

    private <T> List<List<T>> partitionIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}
