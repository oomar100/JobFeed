package io.match.matchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapedJob {

    private String jobTitle;
    private String companyName;
    private String location;
    private Instant datePosted;
    private String salary;
    private String jobUrl;
    private String description;
    private String jobKey;
}