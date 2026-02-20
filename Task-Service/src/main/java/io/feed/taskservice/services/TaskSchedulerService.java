package io.feed.taskservice.services;


import io.feed.taskservice.entity.Task;
import io.feed.taskservice.enums.TaskStatus;
import io.feed.taskservice.kafka.producer.TaskEventProducer;
import io.feed.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskSchedulerService {

    private final TaskRepository taskRepository;
    private final TaskEventProducer eventProducer;

    private static final Duration STUCK_TASK_THRESHOLD = Duration.ofMinutes(30);

    @Scheduled(fixedRateString = "${app.scheduler.poll-interval-ms:60000}")
    @Transactional
    public void processDueTasks() {
        log.debug("Checking for tasks due for execution...");

        Instant now = Instant.now();
        List<Task> dueTasks = taskRepository.findByStatusAndNextRunAtBefore(TaskStatus.SCHEDULED, now);

        if (dueTasks.isEmpty()) {
            log.debug("No tasks due for execution");
            return;
        }

        log.info("Found {} tasks due for execution", dueTasks.size());

        for (Task task : dueTasks) {
            try {
                processTask(task);
            } catch (Exception e) {
                log.error("Error processing task: {}", task.getId(), e);
                task.markAsFailed("Scheduler error: " + e.getMessage(), TaskStatus.SCHEDULE_FAILED);
                taskRepository.save(task);
            }
        }
    }

    private void processTask(Task task) {
        log.info("Processing scheduled task: {}", task.getId());

        task.markAsRunning();
        taskRepository.save(task);

        eventProducer.publishScrapeRequest(task)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish scrape request for scheduled task: {}",
                                task.getId(), ex);
                    } else {
                        log.info("Successfully triggered scheduled task: {}", task.getId());
                    }
                });
    }

    @Scheduled(fixedRate = 900000)
    @Transactional
    public void handleStuckTasks() {
        log.debug("Checking for stuck tasks for cleanup...");

        Instant threshold = Instant.now().minus(STUCK_TASK_THRESHOLD);
        List<Task> stuckTasks = taskRepository.findByStatusInAndLastRunAtBefore(List.of(TaskStatus.SCRAPING, TaskStatus.RANKING), threshold);

        if (stuckTasks.isEmpty()) {
            log.debug("No stuck tasks found");
            return;
        }

        log.warn("Found {} stuck tasks", stuckTasks.size());

        for (Task task : stuckTasks) {
            log.warn("Task {} has been stuck in {} state since {}",
                    task.getId(), task.getStatus(), task.getLastRunAt());

            task.markAsFailed("Task timed out in " + task.getStatus() + " state", TaskStatus.SCHEDULE_FAILED);

            if (!task.isOneTimeRun() && task.getIntervalHours() != null) {
                task.resetForNextRun(Instant.now().plusSeconds(task.getIntervalHours() * 3600L));
            }

            taskRepository.save(task);
        }
    }
}