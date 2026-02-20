package io.scraperworker.kafka.consumer;

import io.scraperworker.kafka.events.ScrapeRequestedEvent;
import io.scraperworker.scraper.IndeedScraper;
import io.scraperworker.scraper.IndeedScraperV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapeEventConsumer {

    private final IndeedScraper indeedScraper;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @KafkaListener(topics = "${app.kafka.topics.scrape-requested}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleScrapeRequested(String message, Acknowledgment ack) {
        try {
            ScrapeRequestedEvent event = objectMapper.readValue(message, ScrapeRequestedEvent.class);
            log.info("Received scrape request for task: {}", event.getTaskId());

            executor.submit(() -> {
                try {
                    indeedScraper.scrape(event);
                }catch (Exception e){
                    log.error("Scrape failed for task: {}", event.getTaskId(), e);
                }
            });

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process scrape request: {}", e.getMessage(), e);
        }
    }
}
