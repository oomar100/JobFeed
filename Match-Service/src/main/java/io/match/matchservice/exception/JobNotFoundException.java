package io.match.matchservice.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID jobId, UUID userId) {
        super("Job not found with id: " + jobId + " for user: " + userId);
    }
}
