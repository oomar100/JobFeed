package io.feed.taskservice.kafka.producer;


import io.feed.taskservice.entity.Task;
import io.feed.taskservice.kafka.events.ScrapeRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper;

    @Value("${app.kafka.topics.scrape-requested}")
    private String scrapeRequestedTopic;

    /**
     * Publishes a scrape request event to trigger the scraper service.
     *
     * @param task The task to be scraped
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, String>> publishScrapeRequest(Task task) {
        ScrapeRequestedEvent event = ScrapeRequestedEvent.builder()
                .taskId(task.getId())
                .userId(task.getUserId())
                .jobTitle(task.getJobTitle())
                .location(task.getLocation())
                .numJobs(task.getNumJobs())
                .datePosted(task.getDatePosted())
                .searchUrl(task.getSearchUrl())
                .skills(task.getSkills())
                .yearsOfExperience(task.getYearsOfExperience())
                .thingsToAvoid(task.getThingsToAvoid())
                .additionalPreferences(task.getAdditionalPreferences())
                .requestedAt(Instant.now())
                .build();

        String key = task.getId().toString();

        String eventJsonStr = mapper.writeValueAsString(event);
        log.info("Publishing scrape request for task: {}", task.getId());
        
        return kafkaTemplate.send(scrapeRequestedTopic, key, eventJsonStr)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish scrape request for task: {}", task.getId(), ex);
                    } else {
                        log.info("Successfully published scrape request for task: {} to partition: {} with offset: {}",
                                task.getId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
