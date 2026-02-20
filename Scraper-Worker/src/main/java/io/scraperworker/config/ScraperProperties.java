package io.scraperworker.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class ScraperProperties {

    private Scrapfly scrapfly = new Scrapfly();
    private Scraper scraper = new Scraper();
    private Kafka kafka = new Kafka();

    @Getter
    @Setter
    public static class Scrapfly {
        private String baseUrl;
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Scraper {
        private int maxAttempts = 4;
        private int pageDelayMs = 2000;
        private int pageDelayRandomMs = 1500;
    }

    @Getter
    @Setter
    public static class Kafka {
        private Topics topics = new Topics();
    }

    @Getter
    @Setter
    public static class Topics {
        private String scrapeRequested;
        private String scrapeCompleted;
        private String scrapeFailed;
    }
}

