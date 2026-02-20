package io.feed.taskservice.dto.request;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    // ============ Search Parameters ============

    @NotBlank(message = "Job title is required")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotNull(message = "Number of jobs is required")
    @Min(value = 1, message = "Must request at least 1 job")
    @Max(value = 500, message = "Cannot request more than 500 jobs")
    private Integer numJobs;

    @Size(max = 2048, message = "Search URL must not exceed 2048 characters")
    private String searchUrl;


    private Integer datePosted;
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
}
