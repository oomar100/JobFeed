package io.feed.taskservice.exception;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(UUID taskId, UUID userId) {
        super("Task not found with id: " + taskId + " for user: " + userId);
    }
}
