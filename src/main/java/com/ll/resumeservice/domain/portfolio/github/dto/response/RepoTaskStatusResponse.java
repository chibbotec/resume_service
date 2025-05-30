package com.ll.resumeservice.domain.portfolio.github.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RepoTaskStatusResponse {
  private String taskId;
  private boolean completed;
  private int progress;
  private int totalFiles;
  private int completedFiles;
  private List<String> savedFiles;
  private List<String> failedFiles;
  private String error;
  private String savedPath;
}

