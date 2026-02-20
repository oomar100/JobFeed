package io.feed.taskservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {

    // ============ Search Parameters ============

    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Min(value = 1, message = "Must request at least 1 job")
    @Max(value = 500, message = "Cannot request more than 500 jobs")
    private Integer numJobs;

    @Size(max = 2048, message = "Search URL must not exceed 2048 characters")
    private String searchUrl;

    // ============ User Preferences ============

    @Size(max = 50, message = "Cannot specify more than 50 skills")
    private List<@Size(max = 100) String> skills;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 50, message = "Years of experience must be realistic")
    private Integer yearsOfExperience;

    @Size(max = 50, message = "Cannot specify more than 50 keywords to avoid")
    private List<@Size(max = 100) String> thingsToAvoid;

    @Size(max = 2000, message = "Additional preferences must not exceed 2000 characters")
    private String additionalPreferences;

    // ============ Schedule Configuration ============

    private Integer intervalHours;

    private Boolean recurring;

    /**
     * If true, pauses the task. If false, resumes a paused task.
     */
    private Boolean paused;
}
