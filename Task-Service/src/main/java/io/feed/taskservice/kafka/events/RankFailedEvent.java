package io.feed.taskservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when ranking fails.
 * Published by the ranker service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankFailedEvent {

    private UUID taskId;
    private UUID userId;
    
    /**
     * Error message describing what went wrong.
     */
    private String error;
    
    /**
     * Whether this failure is retryable.
     */
    private Boolean retryable;
    
    private Instant failedAt;
}
