package io.feed.taskservice;

import io.feed.taskservice.dto.request.CreateTaskRequest;
import io.feed.taskservice.dto.request.UpdateTaskRequest;
import io.feed.taskservice.dto.response.TaskResponse;
import io.feed.taskservice.entity.Task;
import io.feed.taskservice.enums.TaskStatus;
import io.feed.taskservice.exception.InvalidTaskStateException;
import io.feed.taskservice.exception.TaskNotFoundException;
import io.feed.taskservice.kafka.producer.TaskEventProducer;
import io.feed.taskservice.repository.TaskRepository;
import io.feed.taskservice.services.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@Transactional
class TaskServiceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private TaskEventProducer taskEventProducer;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        taskRepository.deleteAll();
    }

    @Nested
    @DisplayName("createTask")
    class CreateTaskTests {

        @Test
        @DisplayName("should create task and run immediately when no intervalHours")
        void createImmediateTask() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .jobTitle("Software Engineer")
                    .location("San Francisco")
                    .numJobs(50)
                    .skills(List.of("Java", "Spring"))
                    .build();

            TaskResponse response = taskService.createTask(userId, request);

            assertThat(response.getId()).isNotNull();
            assertThat(response.getJobTitle()).isEqualTo("Software Engineer");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.SCHEDULED);

            Task saved = taskRepository.findById(response.getId()).orElseThrow();
            assertThat(saved.getUserId()).isEqualTo(userId);
            assertThat(saved.getIntervalHours()).isNull();
        }

        @Test
        @DisplayName("should create scheduled task when intervalHours provided")
        void createScheduledTask() {
            CreateTaskRequest request = CreateTaskRequest.builder()
                    .jobTitle("Backend Developer")
                    .location("Remote")
                    .numJobs(100)
                    .intervalHours(6)
                    .recurring(true)
                    .build();

            TaskResponse response = taskService.createTask(userId, request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.SCHEDULED);
            assertThat(response.getIntervalHours()).isEqualTo(6);
            assertThat(response.getNextRunAt()).isAfter(Instant.now());

            Task saved = taskRepository.findById(response.getId()).orElseThrow();
            assertThat(saved.getRecurring()).isTrue();
        }
    }

    @Nested
    @DisplayName("getTask")
    class GetTaskTests {

        @Test
        @DisplayName("should return task when exists for user")
        void getExistingTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.SCHEDULED);

            TaskResponse response = taskService.getTask(userId, task.getId());

            assertThat(response.getId()).isEqualTo(task.getId());
            assertThat(response.getJobTitle()).isEqualTo("Test Job");
        }

        @Test
        @DisplayName("should throw when task not found")
        void getTaskNotFound() {
            UUID randomId = UUID.randomUUID();

            assertThatThrownBy(() -> taskService.getTask(userId, randomId))
                    .isInstanceOf(TaskNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when task belongs to different user")
        void getTaskWrongUser() {
            Task task = createAndSaveTask(UUID.randomUUID(), "Other User Task", TaskStatus.SCHEDULED);

            assertThatThrownBy(() -> taskService.getTask(userId, task.getId()))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTasks")
    class GetTasksTests {

        @Test
        @DisplayName("should return paginated tasks for user")
        void getTasksPaginated() {
            createAndSaveTask(userId, "Job 1", TaskStatus.SCHEDULED);
            createAndSaveTask(userId, "Job 2", TaskStatus.COMPLETED);
            createAndSaveTask(userId, "Job 3", TaskStatus.FAILED);
            createAndSaveTask(UUID.randomUUID(), "Other User Job", TaskStatus.SCHEDULED);

            Page<TaskResponse> page = taskService.getTasks(userId, PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).extracting(TaskResponse::getUserId)
                    .containsOnly(userId);
        }

        @Test
        @DisplayName("should filter tasks by scheduled status")
        void getTasksByStatus() {
            createAndSaveTask(userId, "Job 1", TaskStatus.SCHEDULED);
            createAndSaveTask(userId, "Job 2", TaskStatus.SCHEDULED);
            createAndSaveTask(userId, "Job 3", TaskStatus.SCRAPING);
            createAndSaveTask(userId, "Job 4", TaskStatus.COMPLETED);

            Page<TaskResponse> page = taskService.getTasksByStatus(userId, TaskStatus.SCHEDULED, PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(TaskResponse::getStatus)
                    .containsOnly(TaskStatus.SCHEDULED);
        }

        @Test
        @DisplayName("should filter tasks by scraping status")
        void getTasksByScrapingStatus() {
            createAndSaveTask(userId, "Job 1", TaskStatus.SCRAPING);
            createAndSaveTask(userId, "Job 2", TaskStatus.SCRAPING);
            createAndSaveTask(userId, "Job 3", TaskStatus.SCRAPING);
            createAndSaveTask(userId, "Job 4", TaskStatus.COMPLETED);

            Page<TaskResponse> page = taskService.getTasksByStatus(userId, TaskStatus.SCRAPING, PageRequest.of(0, 10));

            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).extracting(TaskResponse::getStatus)
                    .containsOnly(TaskStatus.SCRAPING);
        }
    }

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {

        @Test
        @DisplayName("should update task fields")
        void updateTaskFields() {
            Task task = createAndSaveTask(userId, "Old Title", TaskStatus.SCHEDULED);

            UpdateTaskRequest request = UpdateTaskRequest.builder()
                    .jobTitle("New Title")
                    .numJobs(200)
                    .intervalHours(12)
                    .build();

            TaskResponse response = taskService.updateTask(userId, task.getId(), request);

            assertThat(response.getJobTitle()).isEqualTo("New Title");
            assertThat(response.getNumJobs()).isEqualTo(200);
            assertThat(response.getIntervalHours()).isEqualTo(12);
        }

        @Test
        @DisplayName("should pause scheduled task")
        void pauseTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.SCHEDULED);

            UpdateTaskRequest request = UpdateTaskRequest.builder().paused(true).build();

            TaskResponse response = taskService.updateTask(userId, task.getId(), request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.PAUSED);
        }

        @Test
        @DisplayName("should resume paused task")
        void resumeTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.PAUSED);
            task.setIntervalHours(6);
            taskRepository.save(task);

            UpdateTaskRequest request = UpdateTaskRequest.builder().paused(false).build();

            TaskResponse response = taskService.updateTask(userId, task.getId(), request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.SCHEDULED);
            assertThat(response.getNextRunAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should throw when updating task in SCRAPING status")
        void updateScrapingTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.SCRAPING);

            UpdateTaskRequest request = UpdateTaskRequest.builder().jobTitle("New").build();

            assertThatThrownBy(() -> taskService.updateTask(userId, task.getId(), request))
                    .isInstanceOf(InvalidTaskStateException.class);
        }
    }

    @Nested
    @DisplayName("deleteTask")
    class DeleteTaskTests {

        @Test
        @DisplayName("should delete task")
        void deleteTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.COMPLETED);

            taskService.deleteTask(userId, task.getId());

            assertThat(taskRepository.findById(task.getId())).isEmpty();
        }

        @Test
        @DisplayName("should throw when deleting task in RANKING status")
        void deleteRankingTask() {
            Task task = createAndSaveTask(userId, "Test Job", TaskStatus.RANKING);

            assertThatThrownBy(() -> taskService.deleteTask(userId, task.getId()))
                    .isInstanceOf(InvalidTaskStateException.class);
        }
    }

    @Nested
    @DisplayName("Repository: findByStatusAndNextRunAtBefore")
    class FindDueTasksTests {

        @Test
        @DisplayName("should find tasks that are due for execution")
        void findDueTasks() {
            Instant now = Instant.now();

            // Due task - scheduled and nextRunAt in the past
            Task dueTask = createAndSaveTask(userId, "Due Task", TaskStatus.SCHEDULED);
            dueTask.setNextRunAt(now.minus(1, ChronoUnit.HOURS));
            taskRepository.save(dueTask);

            // Not due - scheduled but nextRunAt in the future
            Task futureTask = createAndSaveTask(userId, "Future Task", TaskStatus.SCHEDULED);
            futureTask.setNextRunAt(now.plus(1, ChronoUnit.HOURS));
            taskRepository.save(futureTask);

            // Not due - past nextRunAt but wrong status
            Task completedTask = createAndSaveTask(userId, "Completed Task", TaskStatus.COMPLETED);
            completedTask.setNextRunAt(now.minus(1, ChronoUnit.HOURS));
            taskRepository.save(completedTask);

            List<Task> dueTasks = taskRepository.findByStatusAndNextRunAtBefore(TaskStatus.SCHEDULED, now);

            assertThat(dueTasks).hasSize(1);
            assertThat(dueTasks.get(0).getId()).isEqualTo(dueTask.getId());
        }

        @Test
        @DisplayName("should return empty list when no tasks are due")
        void findNoTasksDue() {
            Task futureTask = createAndSaveTask(userId, "Future Task", TaskStatus.SCHEDULED);
            futureTask.setNextRunAt(Instant.now().plus(1, ChronoUnit.HOURS));
            taskRepository.save(futureTask);

            List<Task> dueTasks = taskRepository.findByStatusAndNextRunAtBefore(TaskStatus.SCHEDULED, Instant.now());

            assertThat(dueTasks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Repository: findByStatusInAndLastRunAtBefore")
    class FindStuckTasksTests {

        @Test
        @DisplayName("should find stuck tasks in SCRAPING or RANKING status")
        void findStuckTasks() {
            Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);

            // Stuck in SCRAPING
            Task stuckScraping = createAndSaveTask(userId, "Stuck Scraping", TaskStatus.SCRAPING);
            stuckScraping.setLastRunAt(Instant.now().minus(1, ChronoUnit.HOURS));
            taskRepository.save(stuckScraping);

            // Stuck in RANKING
            Task stuckRanking = createAndSaveTask(userId, "Stuck Ranking", TaskStatus.RANKING);
            stuckRanking.setLastRunAt(Instant.now().minus(45, ChronoUnit.MINUTES));
            taskRepository.save(stuckRanking);

            // Not stuck - SCRAPING but recent
            Task recentScraping = createAndSaveTask(userId, "Recent Scraping", TaskStatus.SCRAPING);
            recentScraping.setLastRunAt(Instant.now().minus(5, ChronoUnit.MINUTES));
            taskRepository.save(recentScraping);

            // Not stuck - old but COMPLETED status
            Task completedOld = createAndSaveTask(userId, "Completed Old", TaskStatus.COMPLETED);
            completedOld.setLastRunAt(Instant.now().minus(2, ChronoUnit.HOURS));
            taskRepository.save(completedOld);

            List<Task> stuckTasks = taskRepository.findByStatusInAndLastRunAtBefore(
                    List.of(TaskStatus.SCRAPING, TaskStatus.RANKING), threshold);

            assertThat(stuckTasks).hasSize(2);
            assertThat(stuckTasks).extracting(Task::getId)
                    .containsExactlyInAnyOrder(stuckScraping.getId(), stuckRanking.getId());
        }

        @Test
        @DisplayName("should return empty list when no tasks are stuck")
        void findNoStuckTasks() {
            Task recentScraping = createAndSaveTask(userId, "Recent", TaskStatus.SCRAPING);
            recentScraping.setLastRunAt(Instant.now().minus(5, ChronoUnit.MINUTES));
            taskRepository.save(recentScraping);

            Instant threshold = Instant.now().minus(30, ChronoUnit.MINUTES);
            List<Task> stuckTasks = taskRepository.findByStatusInAndLastRunAtBefore(
                    List.of(TaskStatus.SCRAPING, TaskStatus.RANKING), threshold);

            assertThat(stuckTasks).isEmpty();
        }
    }

    private Task createAndSaveTask(UUID userId, String jobTitle, TaskStatus status) {
        Task task = Task.builder()
                .userId(userId)
                .jobTitle(jobTitle)
                .location("Test Location")
                .numJobs(50)
                .status(status)
                .build();
        return taskRepository.save(task);
    }
}