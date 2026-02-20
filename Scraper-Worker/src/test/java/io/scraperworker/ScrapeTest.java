package io.scraperworker;

import io.scraperworker.config.ScraperProperties;
import io.scraperworker.kafka.events.ScrapeRequestedEvent;
import io.scraperworker.scraper.IndeedScraperV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@Import(ScraperProperties.class)
public class ScrapeTest {
    @Autowired
    private IndeedScraperV2 scraper;

    @Test
    void testScrapeJobsWithoutSavingOrPublishing() throws InterruptedException {
//        private UUID taskId;
//        private UUID userId;
//        private String jobTitle;
//        private String location;
//        private Integer numJobs;
//        private String searchUrl;
//        private List<String> skills;
//        private Integer yearsOfExperience;
//        private List<String> thingsToAvoid;
//        private String additionalPreferences;
//        private Instant requestedAt;

        ScrapeRequestedEvent scrapeRequestedEvent = ScrapeRequestedEvent.builder()
                .taskId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .jobTitle("java developer")
                .location("United States")
                .numJobs(5)
                .requestedAt(Instant.now())
                .age(1)
                .build();
        scraper.scrape(scrapeRequestedEvent);

        Thread.sleep(700000);
        // Add assertions on collected data (if exposed or verified indirectly)
    }
}
