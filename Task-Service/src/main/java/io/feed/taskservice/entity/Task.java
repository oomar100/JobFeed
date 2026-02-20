package io.feed.taskservice.entity;


import io.feed.taskservice.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_user_id", columnList = "userId"),
        @Index(name = "idx_tasks_status", columnList = "status"),
        @Index(name = "idx_tasks_next_run_at", columnList = "nextRunAt"),
        @Index(name = "idx_tasks_status_next_run", columnList = "status, nextRunAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    // ============ Search Parameters ============

    @Column(nullable = false, length = 255)
    private String jobTitle;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false)
    private Integer numJobs;

    @Column(length = 2048)
    private String searchUrl;

    private Integer datePosted;

    // ============ User Preferences for Ranking ============

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_skills", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "skill", length = 100)
    @Builder.Default
    private List<String> skills = new ArrayList<>();

    @Column
    private Integer yearsOfExperience;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_avoid_keywords", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "keyword", length = 100)
    @Builder.Default
    private List<String> thingsToAvoid = new ArrayList<>();

    @Column(length = 2000)
    private String additionalPreferences;

    // ============ Schedule Configuration ============

    /**
     * Interval in hours between runs.
     * Null means this is a one-time immediate run.
     */
    @Column
    private Integer intervalHours;

    // ============ Status Tracking ============

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskStatus lastStatus = TaskStatus.SCHEDULED;

    @Column(length = 1000)
    private String errorMessage;

    /**
     * Number of jobs successfully scraped in the last run.
     */
    private Integer lastScrapedCount;

    /**
     * Number of consecutive failures.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer failureCount = 0;

    // ============ Timestamps ============

    private Instant lastRunAt;

    private Instant nextRunAt;

    private Instant completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // ============ Version for Optimistic Locking ============

    @Version
    private Long version;

    // ============ Helper Methods ============

    private Boolean recurring;

    public void markAsRunning() {
        this.status = TaskStatus.SCRAPING;
        this.lastRunAt = Instant.now();
        this.errorMessage = null;
    }

    public void markAsRanking() {
        this.status = TaskStatus.RANKING;
    }

    public void markAsCompleted(int scrapedCount) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.lastScrapedCount = scrapedCount;
        this.failureCount = 0;
        this.errorMessage = null;
    }

    public void markAsFailed(String error, TaskStatus status) {
        this.status = status;
        this.errorMessage = error;
        this.failureCount++;
    }

    public void resetForNextRun(Instant nextRun) {
        this.status = TaskStatus.SCHEDULED;
        this.nextRunAt = nextRun;
        this.errorMessage = null;
    }

    public boolean isOneTimeRun() {
        return intervalHours == null;
    }
}