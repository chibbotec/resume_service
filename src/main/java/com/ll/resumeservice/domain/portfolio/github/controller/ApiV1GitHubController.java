package com.ll.resumeservice.domain.portfolio.github.controller;

import com.ll.resumeservice.domain.portfolio.github.document.GithubCommit;
import com.ll.resumeservice.domain.portfolio.github.dto.request.SaveRepositoryRequest;
import com.ll.resumeservice.domain.portfolio.github.dto.response.GithubApiResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepoTaskStatusResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.TaskResponse;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubApiService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubCommitService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubMongoService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubRepoService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume/{spaceId}/github")
@RequiredArgsConstructor
public class ApiV1GitHubController {

  private final GitHubMongoService gitHubMongoService;
  private final GitHubApiService gitHubApiService;
  private final GitHubRepoService gitHubRepoService;
  private final GitHubCommitService gitHubCommitService;

  @GetMapping("/users/{userId}")
  public ResponseEntity<GithubApiResponse> getGitHubInfo(@PathVariable Long userId) {
    GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
    if (gitHubApi == null) {
      return ResponseEntity.notFound().build();
    }

    // 보안을 위해 토큰 정보는 제외하고 응답
    gitHubApi.setGithubAccessToken("[PROTECTED]");
    return ResponseEntity.ok(GithubApiResponse.from(gitHubApi));
  }

  // MongoDB에서 가져오는 레포지토리 목록 엔드포인트
  @GetMapping("/users/{userId}/db/repositories")
  public ResponseEntity<List<Map<String, Object>>> getUserRepositoriesFromDb(
      @PathVariable Long userId) {
    List<Map<String, Object>> repositories = gitHubMongoService.getRepositoriesFromMongo(userId);
    return ResponseEntity.ok(repositories);
  }

  @PostMapping("/users/{userId}/save-files")
  public ResponseEntity<TaskResponse> saveFilesAsync(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("userId") Long userId,
      @RequestBody SaveRepositoryRequest saveRepositoryRequest
  ) {
    // 비동기 다운로드 작업 시작 및 태스크 ID 반환
    String taskId = gitHubRepoService.startAsyncRepositoryDownload(
        spaceId, userId, saveRepositoryRequest);

    // 즉시 응답 반환
    TaskResponse response = TaskResponse.builder()
        .taskId(taskId)
        .message("다운로드가 백그라운드에서 시작되었습니다. 상태를 확인하려면 '/api/spaces/" +
            spaceId + "/github/tasks/" + taskId + "' 엔드포인트를 사용하세요.")
        .build();

    return ResponseEntity.accepted().body(response);
  }

  @GetMapping("/users/{userId}/db/repositories/{repoOwner}/{repoName}")
  public ResponseEntity<Map<String, Object>> getRepositoryDetailFromDb(
      @PathVariable Long userId,
      @PathVariable("repoOwner") String repoOwner,
      @PathVariable("repoName") String repoName
  ) {
    String fullRepoName = repoOwner + "/" + repoName;
    Map<String, Object> repoDetail = gitHubMongoService.getRepositoryDetailFromMongo(userId,
        fullRepoName);
    return ResponseEntity.ok(repoDetail);
  }

  @GetMapping("/users/{userId}/repositories/{repoOwner}/{repoName}/commit/summary")
  public ResponseEntity<GithubCommit> getCommitSummary(
      @PathVariable("userId") Long userId,
      @PathVariable("repoOwner") String repoOwner,
      @PathVariable("repoName") String repoName
  ){
    GithubCommit allCommit = gitHubCommitService.collectAndSaveCommits(userId, repoOwner, repoName);

    return ResponseEntity.ok(allCommit);
  }

   //수동으로 모든 레포지토리를 다시 MongoDB에 저장합니다.
  @GetMapping("/users/{userId}/sync/repositories")
  public ResponseEntity<String> syncAllRepositoriesToMongo(
      @PathVariable Long userId
  ) {
    try {
      gitHubRepoService.saveRepositoryList(userId);
      return ResponseEntity.ok("모든 GitHub 레포지토리를 성공적으로 MongoDB에 동기화했습니다.");
    } catch (Exception e) {
      log.error("MongoDB 동기화 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body("MongoDB 동기화 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  @GetMapping("/tasks/{taskId}")
  public ResponseEntity<RepoTaskStatusResponse> getTaskStatus(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("taskId") String taskId
  ) {
    RepoTaskStatusResponse status = gitHubRepoService.getTaskStatus(taskId);

    if (status == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(status);
  }
}