package io.match.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiJobRequest {

    private Preferences preferences;
    private List<JobToRank> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Preferences {
        private List<String> skills;
        private Integer yearsOfExperience;
        private List<String> thingsToAvoid;
        private String additionalPreferences;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobToRank {
        private Integer id;
        private String jobTitle;
        private String companyName;
        private String location;
        private String salary;
        private String description;
    }
}