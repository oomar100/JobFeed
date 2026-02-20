package io.feed.taskservice.controller;


import io.feed.taskservice.dto.request.CreateTaskRequest;
import io.feed.taskservice.dto.request.UpdateTaskRequest;
import io.feed.taskservice.dto.response.TaskResponse;
import io.feed.taskservice.enums.TaskStatus;
import io.feed.taskservice.services.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class    TaskController {

    private final TaskService taskService;

    /**
     * Create a new scraping task.
     * If no cron expression is provided, the task runs immediately.
     */
    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateTaskRequest request) {

        log.info("POST /api/v1/tasks - Creating task for user: {}", userId);
        TaskResponse response = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a specific task by ID.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        log.info("GET /api/v1/tasks/{} - User: {}", taskId, userId);
        TaskResponse response = taskService.getTask(userId, taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all tasks for the current user with pagination.
     * Optionally filter by status.
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("GET /api/v1/tasks - User: {}, status: {}", userId, status);

        Page<TaskResponse> response;
        if (status != null) {
            response = taskService.getTasksByStatus(userId, status, pageable);
        } else {
            response = taskService.getTasks(userId, pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing task.
     * Only provided fields will be updated.
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        log.info("PUT /api/v1/tasks/{} - User: {}", taskId, userId);
        TaskResponse response = taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially update a task (same as PUT in this implementation).
     */
    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> patchTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        log.info("PATCH /api/v1/tasks/{} - User: {}", taskId, userId);
        TaskResponse response = taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a task.
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        log.info("DELETE /api/v1/tasks/{} - User: {}", taskId, userId);
        taskService.deleteTask(userId, taskId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually trigger a task to run immediately.
     * Useful for testing or forcing a re-run.
     */
    @PostMapping("/{taskId}/run")
    public ResponseEntity<TaskResponse> runTaskNow(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        log.info("POST /api/v1/tasks/{}/run - Manual trigger by user: {}", taskId, userId);
        TaskResponse response = taskService.runTaskNow(userId, taskId);
        return ResponseEntity.accepted().body(response);
    }

    /**
     * Pause a scheduled task.
     */
    @PostMapping("/{taskId}/pause")
    public ResponseEntity<TaskResponse> pauseTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        log.info("POST /api/v1/tasks/{}/pause - User: {}", taskId, userId);
        UpdateTaskRequest request = UpdateTaskRequest.builder().paused(true).build();
        TaskResponse response = taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resume a paused task.
     */
    @PostMapping("/{taskId}/resume")
    public ResponseEntity<TaskResponse> resumeTask(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID taskId) {

        log.info("POST /api/v1/tasks/{}/resume - User: {}", taskId, userId);
        UpdateTaskRequest request = UpdateTaskRequest.builder().paused(false).build();
        TaskResponse response = taskService.updateTask(userId, taskId, request);
        return ResponseEntity.ok(response);
    }
}
