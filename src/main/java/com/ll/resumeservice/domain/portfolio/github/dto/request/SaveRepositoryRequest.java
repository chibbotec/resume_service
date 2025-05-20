package com.ll.resumeservice.domain.portfolio.github.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class SaveRepositoryRequest {
  private String accessToken;
  private String repository;
  private List<String> filePaths;
  private String branch;
}
