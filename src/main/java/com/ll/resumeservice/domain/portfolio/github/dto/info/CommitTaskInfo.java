package com.ll.resumeservice.domain.portfolio.github.dto.info;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class CommitTaskInfo {
    private int totalCommits;
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failedCount = new AtomicInteger(0);
    private ConcurrentLinkedQueue<String> failedCommits = new ConcurrentLinkedQueue<>();
    private boolean completed = false;
    private String error;
    private long completionTime;
    private List<CommitDetailInfo> results = new ArrayList<>();

    public void incrementSuccess() {
        successCount.incrementAndGet();
    }

    public void incrementFailed() {
        failedCount.incrementAndGet();
    }

    public void addFailedCommit(String commitId) {
        failedCommits.add(commitId);
    }

    public double getProgressPercentage() {
        if (totalCommits == 0) return 0;
        int processed = successCount.get() + failedCount.get();
        return (double) processed / totalCommits * 100;
    }
}
