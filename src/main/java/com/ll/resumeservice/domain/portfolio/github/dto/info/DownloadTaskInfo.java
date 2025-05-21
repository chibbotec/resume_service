package com.ll.resumeservice.domain.portfolio.github.dto.info;

import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DownloadTaskInfo {
  private final AtomicInteger totalFiles = new AtomicInteger(0);
  private final AtomicInteger completedFiles = new AtomicInteger(0);
  private List<String> savedFiles = Collections.synchronizedList(new ArrayList<>());
  private List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());
  private boolean completed = false;
  private String error;
  private Long completionTime;
  private RepositorySaveResponse result;

  public void addToTotal(int count) {
    totalFiles.addAndGet(count);
  }

  public void incrementCompleted() {
    completedFiles.incrementAndGet();
  }

  public int getProgressPercentage() {
    int total = totalFiles.get();
    int completed = completedFiles.get();
    return total > 0 ? (completed * 100 / total) : 0;
  }
}
