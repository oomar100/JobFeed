package io.match.matchservice.dto;

import io.match.matchservice.entity.RankedJob;
import io.match.matchservice.enums.JobBucket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankedJobResponse {

    private UUID id;
    private UUID userId;
    private UUID taskId;
    private String jobTitle;
    private String companyName;
    private String location;
    private Instant datePosted;
    private String salary;
    private String jobUrl;
    private String description;
    private Integer score;
    private JobBucket bucket;
    private Instant createdAt;

    public static RankedJobResponse from(RankedJob entity) {
        return RankedJobResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .taskId(entity.getTaskId())
                .jobTitle(entity.getJobTitle())
                .companyName(entity.getCompanyName())
                .location(entity.getLocation())
                .datePosted(entity.getDatePosted())
                .salary(entity.getSalary())
                .jobUrl(entity.getJobUrl())
                .description(entity.getDescription())
                .score(entity.getScore())
                .bucket(entity.getBucket())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
