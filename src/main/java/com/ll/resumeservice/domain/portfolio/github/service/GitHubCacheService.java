package com.ll.resumeservice.domain.portfolio.github.service;

import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubApiRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubCacheService {

  private final GitHubApiRepository gitHubApiRepository;

//  @Cacheable(value = "githubConnections", key = "#userId")
  public GitHub getGitHubConnection(Long userId) throws IOException {
    // GitHub 연결 로직
    // 이 결과는 캐시에 저장되고 다음 호출 시 재사용됩니다
    log.info("GitHub 연결을 생성합니다: {}", userId);
    GitHubApi gitHubApi = findByUserId(userId);
    return new GitHubBuilder()
        .withOAuthToken(gitHubApi.getGithubAccessToken())
        .build();
  }

//  @Cacheable(value = "githubApis", key = "#userId")
  public GitHubApi findByUserId(Long userId) {
    // DB에서 사용자 정보 조회 로직
    log.info("DB에서 GitHub API 정보를 조회합니다: {}", userId);
    return gitHubApiRepository.findByUserId(userId);
  }
}
