package io.feed.taskservice.exception;


import io.feed.taskservice.enums.TaskStatus;

import java.util.UUID;


public class InvalidTaskStateException extends RuntimeException {

    public InvalidTaskStateException(UUID taskId, TaskStatus currentStatus, String operation) {
        super(String.format("Cannot %s task %s with status %s", operation, taskId, currentStatus));
    }
}
