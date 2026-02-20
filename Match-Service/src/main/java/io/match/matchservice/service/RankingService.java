package io.match.matchservice.service;

import io.match.matchservice.dto.ScrapedJob;
import io.match.matchservice.entity.RankedJob;
import io.match.matchservice.kafka.events.ScrapeCompletedEvent;
import io.match.matchservice.kafka.producer.RankEventProducer;
import io.match.matchservice.repository.RankedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final GeminiService geminiService;
    private final RankedJobRepository rankedJobRepository;
    private final RankEventProducer eventProducer;

    public void rankJobs(ScrapeCompletedEvent event) {
        UUID taskId = event.getTaskId();
        UUID userId = event.getUserId();
        List<ScrapedJob> jobs = event.getJobs();

        log.info("[TaskID: {}] Starting ranking for {} jobs", taskId, jobs.size());

        try {
            // Get all scores in batched calls
            Map<Integer, Integer> scores = geminiService.rankJobs(
                    jobs,
                    event.getSkills(),
                    event.getYearsOfExperience(),
                    event.getThingsToAvoid(),
                    event.getAdditionalPreferences()
            );

            // Build ranked job entities
            List<RankedJob> rankedJobs = new ArrayList<>();
            for (int i = 0; i < jobs.size(); i++) {
                ScrapedJob job = jobs.get(i);
                int score = scores.getOrDefault(i, 5);

                RankedJob rankedJob = RankedJob.builder()
                        .userId(userId)
                        .taskId(taskId)
                        .jobTitle(job.getJobTitle())
                        .companyName(job.getCompanyName())
                        .location(job.getLocation())
                        .datePosted(job.getDatePosted())
                        .salary(job.getSalary())
                        .jobUrl(job.getJobUrl())
                        .description(job.getDescription())
                        .score(score)
                        .build();

                rankedJobs.add(rankedJob);
            }

            rankedJobRepository.saveAll(rankedJobs);
            log.info("[TaskID: {}] Saved {} ranked jobs", taskId, rankedJobs.size());

            eventProducer.publishRankCompleted(taskId, userId, rankedJobs.size());

        } catch (Exception e) {
            log.error("[TaskID: {}] Error during ranking: {}", taskId, e.getMessage(), e);
            eventProducer.publishRankFailed(taskId, userId, "Ranking failed: " + e.getMessage());
        }
    }
}