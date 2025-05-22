package com.ll.resumeservice.domain.portfolio.github.dto.response;

import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubApiResponse {
  private String githubUsername;

  public static GithubApiResponse from(GitHubApi gitHubApi) {
    return GithubApiResponse.builder()
        .githubUsername(gitHubApi.getGithubUsername())
        .build();
  }
}
