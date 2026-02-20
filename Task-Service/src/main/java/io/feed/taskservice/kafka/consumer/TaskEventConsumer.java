package io.feed.taskservice.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.feed.taskservice.entity.Task;
import io.feed.taskservice.enums.TaskStatus;
import io.feed.taskservice.kafka.events.RankCompletedEvent;
import io.feed.taskservice.kafka.events.RankFailedEvent;
import io.feed.taskservice.kafka.events.ScrapeCompletedEvent;
import io.feed.taskservice.kafka.events.ScrapeFailedEvent;
import io.feed.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@EnableKafka
public class TaskEventConsumer {

    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.scrape-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleScrapeCompleted(String message) {
        try {
            ScrapeCompletedEvent event = objectMapper.readValue(message, ScrapeCompletedEvent.class);
            log.info("Received scrape completed event for task: {}, scrapedCount: {}",
                    event.getTaskId(), event.getScrapedCount());

            Optional<Task> optionalTask = taskRepository.findById(event.getTaskId());

            if (optionalTask.isEmpty()) {
                log.warn("Task not found for scrape completed event: {}", event.getTaskId());
                return;
            }

            Task task = optionalTask.get();
            task.markAsRanking();
            task.setLastScrapedCount(event.getScrapedCount());
            task.setLastStatus(TaskStatus.COMPLETED);
            taskRepository.save(task);

            log.info("Task {} transitioned to RANKING status", task.getId());

        } catch (Exception e) {
            log.error("Error processing scrape completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.scrape-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleScrapeFailed(String message) {
        try {
            ScrapeFailedEvent event = objectMapper.readValue(message, ScrapeFailedEvent.class);
            log.warn("Received scrape failed event for task: {}, error: {}",
                    event.getTaskId(), event.getError());

            Optional<Task> optionalTask = taskRepository.findById(event.getTaskId());

            if (optionalTask.isEmpty()) {
                log.warn("Task not found for scrape failed event: {}", event.getTaskId());
                return;
            }

            Task task = optionalTask.get();
            task.markAsFailed(event.getError(), TaskStatus.SCRAPING_FAILED);
            scheduleNextRunIfRecurring(task);
            task.setLastStatus(TaskStatus.SCRAPING_FAILED);
            taskRepository.save(task);

            log.info("Task {} marked as FAILED, task rescheduled ", task.getId());
        } catch (Exception e) {
            log.error("Error processing scrape failed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.rank-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleRankCompleted(String message) {
        try {
            RankCompletedEvent event = objectMapper.readValue(message, RankCompletedEvent.class);
            log.info("Received rank completed event for task: {}, rankedCount: {}",
                    event.getTaskId(), event.getRankedCount());

            Optional<Task> optionalTask = taskRepository.findById(event.getTaskId());

            if (optionalTask.isEmpty()) {
                log.warn("Task not found for rank completed event: {}", event.getTaskId());
                return;
            }

            Task task = optionalTask.get();
            task.markAsCompleted(task.getLastScrapedCount() != null ? task.getLastScrapedCount() : 0);
            scheduleNextRunIfRecurring(task);
            taskRepository.save(task);

            log.info("Task {} marked as COMPLETED", task.getId());

        } catch (Exception e) {
            log.error("Error processing rank completed event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topics.rank-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleRankFailed(String message) {
        try {
            RankFailedEvent event = objectMapper.readValue(message, RankFailedEvent.class);
            log.warn("Received rank failed event for task: {}, error: {}",
                    event.getTaskId(), event.getError());

            Optional<Task> optionalTask = taskRepository.findById(event.getTaskId());

            if (optionalTask.isEmpty()) {
                log.warn("Task not found for rank failed event: {}", event.getTaskId());
                return;
            }

            Task task = optionalTask.get();
            task.markAsFailed("Ranking failed: " + event.getError(), TaskStatus.RANKING);
            task.setLastStatus(TaskStatus.RANKING_FAILED);
            scheduleNextRunIfRecurring(task);
            taskRepository.save(task);

            log.info("Task {} marked as FAILED", task.getId());

        } catch (Exception e) {
            log.error("Error processing rank failed event: {}", e.getMessage(), e);
        }
    }

    private void scheduleNextRunIfRecurring(Task task) {
        if (!task.isOneTimeRun() && task.getIntervalHours() != null) {
            Instant nextRun = Instant.now().plusSeconds(task.getIntervalHours() * 3600L);
            task.setNextRunAt(nextRun);
            task.setStatus(TaskStatus.SCHEDULED);
            log.info("Recurring task {} rescheduled for next run at: {}", task.getId(), nextRun);
        }
    }
}