package com.ll.resumeservice.domain.portfolio.github.eventListener;

import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubApiService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubRepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GithubLoginEventListener {

  private final GitHubApiService gitHubApiService;
  private final GitHubRepoService gitHubRepoService;

  @KafkaListener(
      topics = "github_login",
      groupId = "github_api",
      containerFactory = "kafkaListenerContainerFactory"
  )

  public void listenGithubLoginEvent(GitHubLoginEvent githubLoginEvent) {
    // Handle the event here
    log.info("Received Github login event for user: {}", githubLoginEvent.getUsername());
    log.info("GitHub username: {}", githubLoginEvent.getGithubUsername());
    log.info("GitHub token expires: {}", githubLoginEvent.getGithubTokenExpires());
    log.info("GitHub scopes: {}", githubLoginEvent.getGithubScopes());

    GitHubApi gitHubApi = gitHubApiService.saveGitHubApi(githubLoginEvent);
    log.info("GitHub API 정보 저장 완료: {}", gitHubApi.getGithubAccessToken());

    gitHubRepoService.saveRepositoryList(githubLoginEvent.getUserId());

//    gitHubGraphQLService.saveAllRepositoriesToMongoAsync(githubLoginEvent.getUserId())
//        .thenAccept(result -> log.info("레포지토리 정보 비동기 저장 작업 완료"))
//        .exceptionally(ex -> {
//          log.error("레포지토리 정보 비동기 저장 작업 실패", ex);
//          return null;
//        });

    log.info("GitHub 로그인 이벤트 처리 완료, 레포지토리 정보는 백그라운드에서 저장 중");
  }
}