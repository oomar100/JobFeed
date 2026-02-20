package io.match.matchservice.dto;

import io.match.matchservice.enums.JobBucket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBucketRequest {

    private JobBucket bucket;
}
