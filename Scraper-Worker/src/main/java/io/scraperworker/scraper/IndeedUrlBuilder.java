package io.scraperworker.scraper;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URL;
import java.net.URLEncoder;


@Component
public class IndeedUrlBuilder {

    private static final String INDEED_BASE_URL = "https://www.indeed.com/jobs";

    /**
     * Builds an Indeed search URL.
     *
     * @param jobTitle The job title to search for
     * @param location The location (city, state or zip)
     * @param age      Job posting age in days (1, 3, 7, 14) - null for all
     * @return The constructed Indeed URL
     */
        public String buildSearchUrl(String jobTitle, String location, Integer age) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(INDEED_BASE_URL)
                .queryParam("q", URLEncoder.encode(jobTitle))
                .queryParam("l", URLEncoder.encode(location));

        if (age != null && age > 0) {
            builder.queryParam("fromage", age);
        }

        return builder.build().toUriString();
    }

    /**
     * Builds URL with pagination.
     *
     * @param baseUrl   The base search URL
     * @param startPage The start index (0, 10, 20, etc.)
     * @return URL with start parameter
     */
    public String buildUrlWithPage(String baseUrl, int startPage) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .replaceQueryParam("start", startPage)
                .build(false)
                .toUriString();
    }

    /**
     * Cleans URL by removing view job parameters.
     */
    public String cleanUrl(String url) {
        return UriComponentsBuilder.fromUriString(url)
                .replaceQueryParam("vjk")
                .replaceQueryParam("jk")
                .build(false)
                .toUriString();
    }

    /**
     * Extracts start page from URL.
     */
    public int extractStartPage(String url) {
        try {
            String start = UriComponentsBuilder.fromUriString(url)
                    .build()
                    .getQueryParams()
                    .getFirst("start");
            return start != null ? Integer.parseInt(start) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
