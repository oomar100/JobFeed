package io.feed.taskservice.repository;

import io.feed.taskservice.entity.Task;
import io.feed.taskservice.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByUserId(UUID userId, Pageable pageable);

    Page<Task> findByUserIdAndStatus(UUID userId, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByStatusAndNextRunAtBefore(TaskStatus status, Instant now);
    List<Task> findByStatusInAndLastRunAtBefore(List<TaskStatus> statuses, Instant threshold);
}

