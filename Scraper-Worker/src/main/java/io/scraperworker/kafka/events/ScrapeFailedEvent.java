package io.scraperworker.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeFailedEvent {

    private UUID taskId;
    private UUID userId;
    private String error;
    private Instant failedAt;
}
