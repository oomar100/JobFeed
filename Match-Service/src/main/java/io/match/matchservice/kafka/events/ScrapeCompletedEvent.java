package io.match.matchservice.kafka.events;

import io.match.matchservice.dto.ScrapedJob;
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
    private List<ScrapedJob> jobs;
    private Instant completedAt;

    // User preferences passed along from task-service for ranking
    private List<String> skills;
    private Integer yearsOfExperience;
    private List<String> thingsToAvoid;
    private String additionalPreferences;
}
