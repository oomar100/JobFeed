package io.feed.taskservice.exception;

public class InvalidCronExpressionException extends RuntimeException {

    public InvalidCronExpressionException(String cronExpression) {
        super("Invalid cron expression: " + cronExpression);
    }

    public InvalidCronExpressionException(String cronExpression, Throwable cause) {
        super("Invalid cron expression: " + cronExpression, cause);
    }
}
