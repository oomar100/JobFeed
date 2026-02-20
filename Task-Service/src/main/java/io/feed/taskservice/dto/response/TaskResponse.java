package io.feed.taskservice.dto.response;


import io.feed.taskservice.enums.TaskStatus;
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
public class TaskResponse {

    private UUID id;
    private UUID userId;

    // Search Parameters
    private String jobTitle;
    private String location;
    private Integer numJobs;
    private String searchUrl;
    private Integer datePosted;
    // User Preferences
    private List<String> skills;
    private Integer yearsOfExperience;
    private List<String> thingsToAvoid;
    private String additionalPreferences;

    // Schedule
    private Integer intervalHours;
    private Boolean recurring;

    // Status
    private TaskStatus status;
    private String errorMessage;
    private Integer lastScrapedCount;
    private Integer failureCount;

    // Timestamps
    private Instant lastRunAt;
    private Instant nextRunAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
