package com.ll.resumeservice.domain.portfolio.github.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RepositorySaveResponse {
  private boolean success;
  private List<String> savedFiles;
  private List<String> failedFiles;
  private String savedPath;
}
