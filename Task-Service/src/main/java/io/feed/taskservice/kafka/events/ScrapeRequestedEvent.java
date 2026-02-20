package io.feed.taskservice.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a scraping task should be executed.
 * The scraper service listens for this event.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeRequestedEvent {

    private UUID taskId;
    private UUID userId;

    // Search parameters
    private String jobTitle;
    private String location;
    private String searchUrl;
    private Integer numJobs;
    private Integer datePosted;
    // User preferences (passed along for the ranker)
    private List<String> skills;
    private Integer yearsOfExperience;
    private List<String> thingsToAvoid;
    private String additionalPreferences;

    private Instant requestedAt;
}
