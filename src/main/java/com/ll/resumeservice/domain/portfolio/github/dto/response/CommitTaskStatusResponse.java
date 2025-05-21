package com.ll.resumeservice.domain.portfolio.github.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommitTaskStatusResponse {
    private String taskId;
    private boolean completed;
    private double progress;
    private int totalCommits;
    private int successCount;
    private int failedCount;
    private List<String> failedCommits;
    private String error;
}
