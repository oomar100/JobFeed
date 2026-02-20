package io.scraperworker.kafka.events;

import io.scraperworker.model.Job;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeCompletedEvent {

    private UUID taskId;
    private UUID userId;
    private Integer scrapedCount;
    private List<Job> jobs;
    private Instant completedAt;

    // Pass through from ScrapeRequestedEvent for ranker
    private List<String> skills;
    private Integer yearsOfExperience;
    private List<String> thingsToAvoid;
    private String additionalPreferences;
}
