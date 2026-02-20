package io.feed.taskservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event consumed when scraping completes successfully.
 * Published by the scraper service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeCompletedEvent {

    private UUID taskId;
    private UUID userId;
    
    /**
     * Number of jobs successfully scraped.
     */
    private Integer scrapedCount;
    
    /**
     * Whether this was a partial scrape (less than requested).
     */
    private Boolean partial;
    
    private Instant completedAt;
}
