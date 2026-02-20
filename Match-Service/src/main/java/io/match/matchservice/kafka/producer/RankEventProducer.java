package io.match.matchservice.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.match.matchservice.config.RankerProperties;
import io.match.matchservice.kafka.events.RankCompletedEvent;
import io.match.matchservice.kafka.events.RankFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RankerProperties properties;

    public void publishRankCompleted(UUID taskId, UUID userId, int rankedCount) {
        RankCompletedEvent event = RankCompletedEvent.builder()
                .taskId(taskId)
                .userId(userId)
                .rankedCount(rankedCount)
                .completedAt(Instant.now())
                .build();

        send(properties.getKafka().getTopics().getRankCompleted(), taskId.toString(), event);
        log.info("Published rank completed event for task: {}, rankedCount: {}", taskId, rankedCount);
    }

    public void publishRankFailed(UUID taskId, UUID userId, String error) {
        RankFailedEvent event = RankFailedEvent.builder()
                .taskId(taskId)
                .userId(userId)
                .error(error)
                .failedAt(Instant.now())
                .build();

        send(properties.getKafka().getTopics().getRankFailed(), taskId.toString(), event);
        log.info("Published rank failed event for task: {}, error: {}", taskId, error);
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
