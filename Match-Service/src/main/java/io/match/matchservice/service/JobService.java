package io.match.matchservice.service;

import io.match.matchservice.dto.RankedJobResponse;
import io.match.matchservice.entity.RankedJob;
import io.match.matchservice.enums.JobBucket;
import io.match.matchservice.exception.JobNotFoundException;
import io.match.matchservice.repository.RankedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final RankedJobRepository rankedJobRepository;

    public Page<RankedJobResponse> getAllJobsForUser(UUID userId, Pageable pageable) {
        return rankedJobRepository.findByUserIdOrderByScoreDesc(userId, pageable)
                .map(RankedJobResponse::from);
    }

    public Page<RankedJobResponse> getJobsByBucket(UUID userId, JobBucket bucket, Pageable pageable) {
        return rankedJobRepository.findByUserIdAndBucketOrderByScoreDesc(userId, bucket, pageable)
                .map(RankedJobResponse::from);
    }

    public List<RankedJobResponse> getJobsByTask(UUID userId, UUID taskId) {
        return rankedJobRepository.findByTaskIdOrderByScoreDesc(taskId).stream()
                .filter(job -> job.getUserId().equals(userId))
                .map(RankedJobResponse::from)
                .toList();
    }

    public RankedJobResponse getJob(UUID userId, UUID jobId) {
        RankedJob job = rankedJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException(jobId, userId));
        return RankedJobResponse.from(job);
    }

    @Transactional
    public RankedJobResponse updateBucket(UUID userId, UUID jobId, JobBucket bucket) {
        RankedJob job = rankedJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException(jobId, userId));

        job.setBucket(bucket);
        RankedJob saved = rankedJobRepository.save(job);
        log.info("Updated job {} to bucket {}", jobId, bucket);

        return RankedJobResponse.from(saved);
    }

    @Transactional
    public void deleteJob(UUID userId, UUID jobId) {
        RankedJob job = rankedJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException(jobId, userId));

        rankedJobRepository.delete(job);
        log.info("Deleted job {}", jobId);
    }

    @Transactional
    public void deleteJobsByTask(UUID userId, UUID taskId) {
        List<RankedJob> jobs = rankedJobRepository.findByTaskIdOrderByScoreDesc(taskId);
        List<RankedJob> userJobs = jobs.stream()
                .filter(job -> job.getUserId().equals(userId))
                .toList();

        rankedJobRepository.deleteAll(userJobs);
        log.info("Deleted {} jobs for task {}", userJobs.size(), taskId);
    }

    @Transactional
    public void deleteAllJobsForUser(UUID userId) {
        rankedJobRepository.deleteByUserId(userId);
        log.info("Deleted all jobs for user {}", userId);
    }

    public long getJobCountForUser(UUID userId) {
        return rankedJobRepository.countByUserId(userId);
    }

    public long getJobCountForTask(UUID taskId) {
        return rankedJobRepository.countByTaskId(taskId);
    }
}
