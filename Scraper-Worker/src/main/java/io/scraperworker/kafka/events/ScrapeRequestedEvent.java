package io.scraperworker.kafka.events;

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
public class ScrapeRequestedEvent {

    private UUID taskId;
    private UUID userId;
    private String jobTitle;
    private String location;
    private Integer numJobs;
    private Integer age;
    private List<String> skills;
    private Integer yearsOfExperience;
    private List<String> thingsToAvoid;
    private String additionalPreferences;
    private Instant requestedAt;

    // take a full URL instead
    private String searchUrl;
}
