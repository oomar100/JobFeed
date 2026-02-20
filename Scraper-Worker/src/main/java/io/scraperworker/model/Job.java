package io.scraperworker.model;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Job {
    private String jobTitle;
    private String companyName;
    private String location;
    private Instant datePosted;
    private String salary;
    private String jobUrl;
    private String description;
    private String jobKey;
}

