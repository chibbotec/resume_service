package com.ll.resumeservice.domain.portfolio.github.service;

import com.ll.resumeservice.domain.portfolio.github.eventListener.GitHubLoginEvent;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class GitHubApiService {

  private final GitHubApiRepository gitHubApiRepository;

  @Transactional
  public GitHubApi saveGitHubApi(GitHubLoginEvent gitHubLoginEvent) {
    // 기존 데이터가 있는지 확인
    GitHubApi existingApi = gitHubApiRepository.findByUserId(gitHubLoginEvent.getUserId());

    if (existingApi != null) {
      // 기존 데이터 업데이트
      existingApi.setGithubUsername(gitHubLoginEvent.getGithubUsername());
      existingApi.setGithubAccessToken(gitHubLoginEvent.getGithubAccessToken());
      existingApi.setGithubScopes(gitHubLoginEvent.getGithubScopes());
      existingApi.setGithubTokenExpires(gitHubLoginEvent.getGithubTokenExpires());
      return gitHubApiRepository.save(existingApi);
    } else {
      // 새 데이터 생성
      GitHubApi gitHubApi = GitHubApi.builder()
          .userId(gitHubLoginEvent.getUserId())
          .username(gitHubLoginEvent.getUsername())
          .email(gitHubLoginEvent.getEmail())
          .nickname(gitHubLoginEvent.getNickname())
          .githubUsername(gitHubLoginEvent.getGithubUsername())
          .githubAccessToken(gitHubLoginEvent.getGithubAccessToken())
          .githubTokenExpires(gitHubLoginEvent.getGithubTokenExpires())
          .githubScopes(gitHubLoginEvent.getGithubScopes())
          .providerId(gitHubLoginEvent.getProviderId())
          .providerType(gitHubLoginEvent.getProviderType())
          .build();

      return gitHubApiRepository.save(gitHubApi);
    }
  }
}