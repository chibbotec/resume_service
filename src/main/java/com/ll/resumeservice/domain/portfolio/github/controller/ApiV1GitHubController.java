package com.ll.resumeservice.domain.portfolio.github.controller;

import com.ll.resumeservice.domain.portfolio.github.dto.SaveRepositoryContents;
import com.ll.resumeservice.domain.portfolio.github.dto.response.GithubApiResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubApiService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubCacheService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubMongoService;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubRepositoryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume/{spaceId}/github")
@RequiredArgsConstructor
public class ApiV1GitHubController {

  private final GitHubApiService gitHubApiService;
  private final GitHubMongoService gitHubMongoService;
  private final GitHubCacheService cacheService;
  private final GitHubRepositoryService gitHubRepositoryService;

  @GetMapping("/users/{userId}")
  public ResponseEntity<GithubApiResponse> getGitHubInfo(@PathVariable Long userId) {
    GitHubApi gitHubApi = cacheService.findByUserId(userId);
    log.debug("================log:{}",gitHubApi.getGithubUsername());
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
  public ResponseEntity<RepositorySaveResponse> saveFiles(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("userId") Long userId,
      @RequestBody SaveRepositoryContents saveRepositoryContents
  ) {
    RepositorySaveResponse response = gitHubRepositoryService.saveRepositoryContents(
        spaceId, userId, saveRepositoryContents);

    if (response.isSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  /******************* 아래는 개발 필요 *****************/

  /**
   * MongoDB에서 레포지토리 컨텐츠를 조회합니다.
   */
  @GetMapping("/users/{userId}/db/repositories/{repoOwner}/{repoName}/contents/**")
  public ResponseEntity<List<Map<String, Object>>> getRepositoryContentsFromDb(
      @PathVariable Long userId,
      @PathVariable String repoOwner,
      @PathVariable String repoName,
      HttpServletRequest request) {

    // URL에서 contents/ 이후의 경로 추출
    String path = "";
    String requestURL = request.getRequestURI();
    String[] pathSegments = requestURL.split("/contents/");

    if (pathSegments.length > 1) {
      path = pathSegments[1];
    }

    log.info("DB fetch - repoOwner: {}", repoOwner);
    log.info("DB fetch - repoName: {}", repoName);
    log.info("DB fetch - extracted path: {}", path);

    // owner/repo 형태로 전체 레포지토리 이름 구성
    String fullRepoName = repoOwner + "/" + repoName;

    List<Map<String, Object>> contents = gitHubMongoService.getRepositoryContentsFromMongo(userId,
        fullRepoName, path);
    return ResponseEntity.ok(contents);
  }

  /**
   * MongoDB에서 특정 저장소의 상세 정보를 조회합니다.
   */
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

  /**
   * 수동으로 모든 레포지토리를 다시 MongoDB에 저장합니다.
   */
  @GetMapping("/users/{userId}/sync/repositories")
  public ResponseEntity<String> syncAllRepositoriesToMongo(
      @PathVariable Long userId
  ) {
    try {
      gitHubApiService.saveAllRepositoriesToMongo(userId);
      return ResponseEntity.ok("모든 GitHub 레포지토리를 성공적으로 MongoDB에 동기화했습니다.");
    } catch (Exception e) {
      log.error("MongoDB 동기화 중 오류 발생", e);
      return ResponseEntity.internalServerError()
          .body("MongoDB 동기화 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  /******************* 아래는 API 직접 호출 엔드포인트 (사용되지 않음) *****************/

  @GetMapping("/users/{userId}/repositories")
  public ResponseEntity<List<Map<String, Object>>> getUserRepositories(@PathVariable Long userId) {
    List<Map<String, Object>> repositories = gitHubApiService.getUserRepositories(userId);
    return ResponseEntity.ok(repositories);
  }

  @GetMapping("/users/{userId}/repositories/{repoOwner}/{repoName}/contents/**")
  public ResponseEntity<List<Map<String, Object>>> getRepositoryContents(
      @PathVariable Long userId,
      @PathVariable String repoOwner,
      @PathVariable String repoName,
      HttpServletRequest request) {

    // URL에서 contents/ 이후의 경로 추출
    String path = "";
    String requestURL = request.getRequestURI();
    String[] pathSegments = requestURL.split("/contents/");

    if (pathSegments.length > 1) {
      path = pathSegments[1];
    }

    log.info("repoOwner: {}", repoOwner);
    log.info("repoName: {}", repoName);
    log.info("extracted path: {}", path);

    // owner/repo 형태로 전체 레포지토리 이름 구성
    String fullRepoName = repoOwner + "/" + repoName;

    List<Map<String, Object>> contents = gitHubApiService.getRepositoryContents(userId,
        fullRepoName, path);
    return ResponseEntity.ok(contents);
  }

  @GetMapping("/users/{userId}/repositories/{repoOwner}/{repoName}")
  public ResponseEntity<Map<String, Object>> getRepositoryDetail(
      @PathVariable Long userId,
      @PathVariable("repoOwner") String repoOwner,
      @PathVariable("repoName") String repoName
  ) {
    String fullRepoName = repoOwner + "/" + repoName;
    Map<String, Object> repoDetail = gitHubApiService.getRepositoryDetail(userId, fullRepoName);
    return ResponseEntity.ok(repoDetail);
  }

  @GetMapping("/users/{userId}/repositories/{repoName}/commits")
  public ResponseEntity<List<Map<String, Object>>> getRepositoryCommits(
      @PathVariable Long userId,
      @PathVariable String repoName,
      @RequestParam(defaultValue = "30") int maxCount) {
    List<Map<String, Object>> commits = gitHubApiService.getRepositoryCommits(userId, repoName,
        maxCount);
    return ResponseEntity.ok(commits);
  }

  @GetMapping("/users/{userId}/repositories/{repoName}/stats")
  public ResponseEntity<Map<String, Object>> getRepositoryStats(
      @PathVariable Long userId,
      @PathVariable String repoName) {
    Map<String, Object> stats = gitHubApiService.getRepositoryStats(userId, repoName);
    return ResponseEntity.ok(stats);
  }
}