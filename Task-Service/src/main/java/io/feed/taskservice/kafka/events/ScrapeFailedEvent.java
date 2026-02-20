package io.feed.taskservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when scraping fails completely.
 * Published by the scraper service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeFailedEvent {

    private UUID taskId;
    private UUID userId;

    private String error;

    private Boolean retryable;
    
    private Instant failedAt;
}
