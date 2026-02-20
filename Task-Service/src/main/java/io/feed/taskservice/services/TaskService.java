package io.feed.taskservice.services;

import io.feed.taskservice.dto.request.CreateTaskRequest;
import io.feed.taskservice.dto.request.UpdateTaskRequest;
import io.feed.taskservice.dto.response.TaskResponse;
import io.feed.taskservice.entity.Task;
import io.feed.taskservice.enums.TaskStatus;
import io.feed.taskservice.exception.InvalidTaskStateException;
import io.feed.taskservice.exception.TaskNotFoundException;
import io.feed.taskservice.kafka.producer.TaskEventProducer;
import io.feed.taskservice.mapper.TaskMapper;
import io.feed.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskEventProducer eventProducer;

    /**
     * Creates a new task. If no intervalHours is provided, the task runs immediately.
     * If intervalHours is provided, it's scheduled for that interval.
     */
    @Transactional
    public TaskResponse createTask(UUID userId, CreateTaskRequest request) {
        log.info("Creating task for user: {}, jobTitle: {}", userId, request.getJobTitle());

        Task task = taskMapper.toEntity(request, userId);

        boolean isScheduled = request.getIntervalHours() != null && request.getIntervalHours() > 0;

        if (isScheduled) {
            Instant nextRun = Instant.now().plusSeconds(request.getIntervalHours() * 3600L);
            task.setNextRunAt(nextRun);
            task.setRecurring(true);
            task.setStatus(TaskStatus.SCHEDULED);
            log.info("Task scheduled for: {}", nextRun);
        } else {
            task.setRecurring(false);
            task.setNextRunAt(Instant.now().plusSeconds(60L));
            task.setStatus(TaskStatus.SCHEDULED);
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task created with id: {}", savedTask.getId());

        return taskMapper.toResponse(savedTask);
    }

    /**
     * Gets a task by ID for a specific user.
     */
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID userId, UUID taskId) {
        Task task = findTaskForUser(taskId, userId);
        return taskMapper.toResponse(task);
    }

    /**
     * Gets all tasks for a user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(UUID userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable)
                .map(taskMapper::toResponse);
    }

    /**
     * Gets tasks for a user filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByStatus(UUID userId, TaskStatus status, Pageable pageable) {
        return taskRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(taskMapper::toResponse);
    }

    /**
     * Updates an existing task. Only non-null fields in the request will be updated.
     */
    @Transactional
    public TaskResponse updateTask(UUID userId, UUID taskId, UpdateTaskRequest request) {
        log.info("Updating task: {} for user: {}", taskId, userId);

        Task task = findTaskForUser(taskId, userId);

        if (task.getStatus() == TaskStatus.SCRAPING || task.getStatus() == TaskStatus.RANKING) {
            throw new InvalidTaskStateException(taskId, task.getStatus(), "update");
        }

        task.setJobTitle(request.getJobTitle());
        task.setLocation(request.getLocation());
        task.setNumJobs(request.getNumJobs());
        task.setSearchUrl(request.getSearchUrl());
        task.setSkills(request.getSkills());
        task.setYearsOfExperience(request.getYearsOfExperience());
        task.setThingsToAvoid(request.getThingsToAvoid());
        task.setThingsToAvoid(request.getThingsToAvoid());
        task.setAdditionalPreferences(request.getAdditionalPreferences());
        task.setRecurring(request.getRecurring());

        if (request.getIntervalHours() != null) {
            if (request.getIntervalHours() <= 0) {
                task.setIntervalHours(null);
                task.setRecurring(false);
            } else {
                task.setIntervalHours(request.getIntervalHours());
                task.setNextRunAt(Instant.now().plusSeconds(request.getIntervalHours() * 3600L));
            }
        }

        if (request.getPaused() != null) {
            if (request.getPaused() && task.getStatus() == TaskStatus.SCHEDULED) {
                task.setStatus(TaskStatus.PAUSED);
                log.info("Task {} paused", taskId);
            } else if (!request.getPaused() && task.getStatus() == TaskStatus.PAUSED) {
                task.setStatus(TaskStatus.SCHEDULED);
                if (task.getIntervalHours() != null) {
                    task.setNextRunAt(Instant.now().plusSeconds(task.getIntervalHours() * 3600L));
                }
                log.info("Task {} resumed, next run: {}", taskId, task.getNextRunAt());
            }
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task {} updated successfully", taskId);

        return taskMapper.toResponse(savedTask);
    }

    /**
     * Deletes a task.
     */
    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        log.info("Deleting task: {} for user: {}", taskId, userId);

        Task task = findTaskForUser(taskId, userId);

        if (task.getStatus() == TaskStatus.SCRAPING || task.getStatus() == TaskStatus.RANKING) {
            throw new InvalidTaskStateException(taskId, task.getStatus(), "delete");
        }

        taskRepository.delete(task);
        log.info("Task {} deleted successfully", taskId);
    }

    /**
     * Manually triggers a task to run immediately.
     */
    @Transactional
    public TaskResponse runTaskNow(UUID userId, UUID taskId) {
        log.info("Manual run requested for task: {} by user: {}", taskId, userId);

        Task task = findTaskForUser(taskId, userId);

        if (task.getStatus() != TaskStatus.SCHEDULED
                && task.getStatus() != TaskStatus.COMPLETED
                && task.getStatus() != TaskStatus.SCHEDULE_FAILED
                && task.getStatus() != TaskStatus.PAUSED) {
            throw new InvalidTaskStateException(taskId, task.getStatus(), "run now");
        }

        triggerScrape(task);
        return taskMapper.toResponse(task);
    }

    /**
     * Triggers the scraping process for a task.
     */
    private void triggerScrape(Task task) {
        task.markAsRunning();
        taskRepository.save(task);

        eventProducer.publishScrapeRequest(task)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish scrape request for task: {}", task.getId(), ex);
                    }
                });
    }

    /**
     * Helper method to find a task ensuring it belongs to the user.
     */
    private Task findTaskForUser(UUID taskId, UUID userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new TaskNotFoundException(taskId, userId));
    }
}