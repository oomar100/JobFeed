package io.feed.taskservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when ranking completes successfully.
 * Published by the ranker service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankCompletedEvent {

    private UUID taskId;
    private UUID userId;
    
    /**
     * Number of jobs that were ranked.
     */
    private Integer rankedCount;
    
    private Instant completedAt;
}
