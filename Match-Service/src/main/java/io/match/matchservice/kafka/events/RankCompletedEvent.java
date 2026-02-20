package io.match.matchservice.kafka.events;

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
public class RankCompletedEvent {

    private UUID taskId;
    private UUID userId;
    private Integer rankedCount;
    private Instant completedAt;
}
