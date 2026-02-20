package io.match.matchservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class RankerProperties {

    private Kafka kafka = new Kafka();
    private Gemini gemini = new Gemini();

    @Getter
    @Setter
    public static class Kafka {
        private Topics topics = new Topics();
    }

    @Getter
    @Setter
    public static class Topics {
        private String scrapeCompleted;
        private String rankCompleted;
        private String rankFailed;
    }

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey;
        private String model;
        private String baseUrl;
    }
}
