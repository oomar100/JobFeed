package io.match.matchservice.repository;


import io.match.matchservice.entity.RankedJob;
import io.match.matchservice.enums.JobBucket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RankedJobRepository extends JpaRepository<RankedJob, UUID> {

    Page<RankedJob> findByUserIdOrderByScoreDesc(UUID userId, Pageable pageable);

    Page<RankedJob> findByUserIdAndBucketOrderByScoreDesc(UUID userId, JobBucket bucket, Pageable pageable);

    List<RankedJob> findByTaskIdOrderByScoreDesc(UUID taskId);

    Optional<RankedJob> findByIdAndUserId(UUID id, UUID userId);

    void deleteByTaskId(UUID taskId);

    void deleteByUserId(UUID userId);

    long countByTaskId(UUID taskId);

    long countByUserId(UUID userId);
}
