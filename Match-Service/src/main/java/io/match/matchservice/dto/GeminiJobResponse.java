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
public class GeminiJobResponse {

    private List<JobScore> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobScore {
        private Integer id;
        private Integer score;
    }
}