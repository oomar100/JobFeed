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
public class RankFailedEvent {

    private UUID taskId;
    private UUID userId;
    private String error;
    private Instant failedAt;
}
