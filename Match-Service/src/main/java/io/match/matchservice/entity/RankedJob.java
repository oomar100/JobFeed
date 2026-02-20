package io.match.matchservice.entity;

import io.match.matchservice.enums.JobBucket;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ranked_jobs", indexes = {
        @Index(name = "idx_ranked_jobs_user_id", columnList = "userId"),
        @Index(name = "idx_ranked_jobs_task_id", columnList = "taskId"),
        @Index(name = "idx_ranked_jobs_user_bucket", columnList = "userId, bucket"),
        @Index(name = "idx_ranked_jobs_user_score", columnList = "userId, score DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private String jobTitle;

    private String companyName;

    private String location;

    private Instant datePosted;

    private String salary;

    @Column(length = 2048)
    private String jobUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobBucket bucket = JobBucket.NONE;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
