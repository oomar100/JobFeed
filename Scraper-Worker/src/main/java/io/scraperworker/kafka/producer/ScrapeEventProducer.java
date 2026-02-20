package io.scraperworker.kafka.producer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.scraperworker.config.ScraperProperties;
import io.scraperworker.kafka.events.ScrapeRequestedEvent;
import io.scraperworker.model.Job;
import io.scraperworker.kafka.events.ScrapeCompletedEvent;
import io.scraperworker.kafka.events.ScrapeFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapeEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ScraperProperties properties;

    public void publishScrapeCompleted(ScrapeRequestedEvent originalRequest, List<Job> jobs) {
        ScrapeCompletedEvent event = ScrapeCompletedEvent.builder()
                .taskId(originalRequest.getTaskId())
                .userId(originalRequest.getUserId())
                .scrapedCount(jobs.size())
                .jobs(jobs)
                .completedAt(Instant.now())
                // Pass through preferences
                .skills(originalRequest.getSkills())
                .yearsOfExperience(originalRequest.getYearsOfExperience())
                .thingsToAvoid(originalRequest.getThingsToAvoid())
                .additionalPreferences(originalRequest.getAdditionalPreferences())
                .build();

        send(properties.getKafka().getTopics().getScrapeCompleted(), originalRequest.getTaskId().toString(), event);
        log.info("Published scrape completed event for task: {}, jobCount: {}", originalRequest.getTaskId(), jobs.size());
    }

    public void publishScrapeFailed(UUID taskId, UUID userId, String error) {
        ScrapeFailedEvent event = ScrapeFailedEvent.builder()
                .taskId(taskId)
                .userId(userId)
                .error(error)
                .failedAt(Instant.now())
                .build();

        send(properties.getKafka().getTopics().getScrapeFailed(), taskId.toString(), event);
        log.info("Published scrape failed event for task: {}, error: {}", taskId, error);
    }

    private void send(String topic, String key, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}