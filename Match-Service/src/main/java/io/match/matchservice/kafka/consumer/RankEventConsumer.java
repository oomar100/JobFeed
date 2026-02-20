package io.match.matchservice.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.match.matchservice.kafka.events.ScrapeCompletedEvent;
import io.match.matchservice.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankEventConsumer {

    private final ObjectMapper objectMapper;
    private final RankingService rankingService;

    @KafkaListener(topics = "${app.kafka.topics.scrape-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleScrapeCompleted(String message) {
        try {
            ScrapeCompletedEvent event = objectMapper.readValue(message, ScrapeCompletedEvent.class);
            log.info("Received scrape completed event for task: {}, jobCount: {}",
                    event.getTaskId(), event.getScrapedCount());

            rankingService.rankJobs(event);

        } catch (Exception e) {
            log.error("Failed to process scrape completed event: {}", e.getMessage(), e);
        }
    }
}
