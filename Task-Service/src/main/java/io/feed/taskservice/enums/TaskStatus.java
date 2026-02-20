package io.feed.taskservice.enums;


public enum TaskStatus {
    /**
     * Task is scheduled and waiting for its next run time
     */
    SCHEDULED,

    /**
     * Task is currently being scraped
     */
    SCRAPING,

    /**
     * Scraping completed, jobs are being ranked
     */
    RANKING,

    /**
     * Task completed successfully
     */
    COMPLETED,

    /**
     * Task failed during scraping
     */
    SCRAPING_FAILED,

    /**
     * Task failed during ranking
     */
    RANKING_FAILED,

    /**
     * Task failed during scheduling errors
     */
    SCHEDULE_FAILED,

    /**
     * Task is paused and won't run until resumed
     */
    PAUSED
}
