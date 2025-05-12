package com.ll.resumeservice.domain.portfolio.github.dto;

import java.util.List;
import lombok.Data;

@Data
public class SaveRepositoryContents {
  private String accessToken;
  private String repository;
  private List<String> filePaths;
  private String branch;
}
