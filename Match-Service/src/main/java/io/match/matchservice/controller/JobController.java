package io.match.matchservice.controller;


import io.match.matchservice.dto.RankedJobResponse;
import io.match.matchservice.dto.UpdateBucketRequest;
import io.match.matchservice.enums.JobBucket;
import io.match.matchservice.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public Page<RankedJobResponse> getAllJobs(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) JobBucket bucket,
            Pageable pageable) {

        if (bucket != null) {
            return jobService.getJobsByBucket(userId, bucket, pageable);
        }
        return jobService.getAllJobsForUser(userId, pageable);
    }

    @GetMapping("/task/{taskId}")
    public List<RankedJobResponse> getJobsByTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        return jobService.getJobsByTask(userId, taskId);
    }

    @GetMapping("/{jobId}")
    public RankedJobResponse getJob(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID jobId) {

        return jobService.getJob(userId, jobId);
    }

    @PatchMapping("/{jobId}/bucket")
    public RankedJobResponse updateBucket(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID jobId,
            @RequestBody UpdateBucketRequest request) {

        return jobService.updateBucket(userId, jobId, request.getBucket());
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> deleteJob(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID jobId) {

        jobService.deleteJob(userId, jobId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteJobsByTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        jobService.deleteJobsByTask(userId, taskId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllJobs(
            @RequestHeader("X-User-Id") UUID userId) {

        jobService.deleteAllJobsForUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public Map<String, Long> getJobCount(
            @RequestHeader("X-User-Id") UUID userId) {

        return Map.of("count", jobService.getJobCountForUser(userId));
    }

    @GetMapping("/task/{taskId}/count")
    public Map<String, Long> getJobCountForTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        return Map.of("count", jobService.getJobCountForTask(taskId));
    }
}
