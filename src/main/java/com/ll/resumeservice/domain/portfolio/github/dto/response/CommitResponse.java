package com.ll.resumeservice.domain.portfolio.github.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitResponse {

  private List<CommitFiles> commitFiles;


  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommitFiles {

    private String repository;
    private List<String> commitFiles;
  }
}
