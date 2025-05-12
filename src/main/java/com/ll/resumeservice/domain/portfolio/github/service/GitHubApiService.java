package com.ll.resumeservice.domain.portfolio.github.service;

import com.ll.resumeservice.domain.portfolio.github.dto.GitHubLoginEvent;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubApiRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class GitHubApiService {

  private final GitHubApiRepository gitHubApiRepository;
  private final GitHubCacheService cacheService;
  private final GitHubMongoService gitHubMongoService;

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

  public List<Map<String, Object>> getUserRepositories(Long userId) {
    try {
      GitHub github = cacheService.getGitHubConnection(userId);
      return github.getMyself().listRepositories().toList().stream()
          .map(repo -> {
            Map<String, Object> repoMap = new HashMap<>();
            repoMap.put("id", repo.getId());
            repoMap.put("name", repo.getName());
            repoMap.put("fullName", repo.getFullName());
            repoMap.put("description", repo.getDescription() != null ? repo.getDescription() : "");
            repoMap.put("url", repo.getHtmlUrl().toString());
            repoMap.put("defaultBranch", repo.getDefaultBranch());
            try {
              repoMap.put("language", repo.getLanguage());
              repoMap.put("fork", repo.isFork());
              repoMap.put("stars", repo.getStargazersCount());
              repoMap.put("forks", repo.getForksCount());
              repoMap.put("createdAt", repo.getCreatedAt());
              repoMap.put("updatedAt", repo.getUpdatedAt());
              repoMap.put("pushedAt", repo.getPushedAt());
              repoMap.put("size", repo.getSize());
            } catch (IOException e) {
              log.warn("레포지토리 추가 정보 조회 중 오류", e);
            }
            return repoMap;
          })
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.error("GitHub API 호출 중 오류 발생", e);
      throw new RuntimeException("GitHub 저장소 목록을 가져오는데 실패했습니다", e);
    }
  }

  public List<Map<String, Object>> getRepositoryContents(Long userId, String repoName, String path) {
    try {
      GitHub github = cacheService.getGitHubConnection(userId);
      GitHubApi gitHubApi = cacheService.findByUserId(userId);

      // 레포지토리 이름 처리 로직 개선
      String fullRepoName;
      if (repoName.contains("/")) {
        fullRepoName = repoName;
      } else {
        fullRepoName = gitHubApi.getGithubUsername() + "/" + repoName;
      }

      log.info("Repository fetch: {}, path: {}", fullRepoName, path);
      GHRepository repo = github.getRepository(fullRepoName);

      List<Map<String, Object>> contents = new ArrayList<>();

      try {
        List<GHContent> contentList;
        // path가 null이나 빈 문자열인 경우 처리
        if (path == null || path.trim().isEmpty()) {
          contentList = repo.getDirectoryContent("/");
        } else {
          // 앞에 '/'가 없으면 추가
          contentList = repo.getDirectoryContent(path.startsWith("/") ? path : "/" + path);
        }

        for (GHContent content : contentList) {
          Map<String, Object> item = new HashMap<>();
          item.put("name", content.getName());
          item.put("path", content.getPath());
          item.put("type", content.isDirectory() ? "dir" : "file");
          item.put("size", content.getSize());
          // null 체크를 추가하여 안전하게 URL 처리
          item.put("url", content.getHtmlUrl() != null ? content.getHtmlUrl().toString() : null);
          item.put("downloadUrl", content.isDirectory() ? null :
              (content.getDownloadUrl() != null ? content.getDownloadUrl().toString() : null));
          item.put("sha", content.getSha());

          if(content.isDirectory()) {
            item.put("directory", getRepositoryContents(userId, fullRepoName, content.getPath()));
          }

          contents.add(item);
        }
      } catch (GHFileNotFoundException e) {
        // 레포지토리가 비어 있는 경우
        log.info("레포지토리가 비어 있습니다: {}", fullRepoName);
        // 빈 목록 반환 (이미 contents가 빈 리스트로 초기화되어 있음)
      } catch (IOException e) {
        // 다른 IO 예외가 발생한 경우
        log.error("디렉토리 내용 조회 중 오류 발생", e);
        throw e; // 상위 catch 블록으로 예외 전파
      }

      return contents;
    } catch (IOException e) {
      log.error("GitHub 레포지토리 내용 조회 중 오류 발생", e);

      // GitHub API의 상세 에러 메시지 추출 및 로깅
      String errorMessage = e.getMessage();
      if (e.getCause() != null) {
        errorMessage += " - " + e.getCause().getMessage();
      }
      log.error("GitHub API 에러 상세 정보: {}", errorMessage);

      // 빈 레포지토리 여부 확인
      if (errorMessage.contains("empty") ||
          (e instanceof GHFileNotFoundException && errorMessage.contains("404"))) {
        log.info("빈 레포지토리로 판단됨: {}", repoName);
        return new ArrayList<>(); // 빈 레포지토리인 경우 빈 목록 반환
      }

      throw new RuntimeException("GitHub 레포지토리 내용을 가져오는데 실패했습니다: " + repoName + " 경로: " + path, e);
    }
  }

  public Map<String, Object> getRepositoryDetail(Long userId, String repoName) {
    try {
      GitHub github = cacheService.getGitHubConnection(userId);

      // 사용자의 GitHub 아이디와 레포지토리 이름으로 전체 레포지토리 이름 구성
      GHRepository repo = github.getRepository(repoName);

      Map<String, Object> repoDetail = new HashMap<>();
      repoDetail.put("id", repo.getId());
      repoDetail.put("name", repo.getName());
      repoDetail.put("fullName", repo.getFullName());
      repoDetail.put("description", repo.getDescription() != null ? repo.getDescription() : "");
      repoDetail.put("url", repo.getHtmlUrl().toString());
      repoDetail.put("apiUrl", repo.getUrl());
      repoDetail.put("gitUrl", repo.getGitTransportUrl());
      repoDetail.put("sshUrl", repo.getSshUrl());
      repoDetail.put("homepage", repo.getHomepage());
      repoDetail.put("language", repo.getLanguage());
      repoDetail.put("defaultBranch", repo.getDefaultBranch());
      repoDetail.put("private", repo.isPrivate());
      repoDetail.put("fork", repo.isFork());
      repoDetail.put("archived", repo.isArchived());
      repoDetail.put("disabled", repo.isDisabled());
      repoDetail.put("stars", repo.getStargazersCount());
      repoDetail.put("watchers", repo.getWatchersCount());
      repoDetail.put("forks", repo.getForksCount());
      repoDetail.put("size", repo.getSize());
      repoDetail.put("createdAt", repo.getCreatedAt());
      repoDetail.put("updatedAt", repo.getUpdatedAt());
      repoDetail.put("pushedAt", repo.getPushedAt());
      repoDetail.put("files", getRepositoryContents(userId, repoName, null));

//      // 필요에 따라 README 내용 가져오기
//      try {
//        repoDetail.put("readme", repo.getReadme().getContent());
//      } catch (Exception e) {
//        log.info("README를 찾을 수 없습니다: {}", fullRepoName);
//        repoDetail.put("readme", "");
//      }

      gitHubMongoService.saveRepositoryToMongo(userId, repoDetail);

      return repoDetail;
    } catch (IOException e) {
      log.error("GitHub 레포지토리 상세 정보 조회 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 상세 정보를 가져오는데 실패했습니다: " + repoName, e);
    }
  }

  public void saveAllRepositoriesToMongo(Long userId) {
    try {
      List<Map<String, Object>> repositories = getUserRepositories(userId);
      for (Map<String, Object> repo : repositories) {
        getRepositoryDetail(userId, repo.get("fullName").toString());
      }
    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보를 MongoDB에 저장하는 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

  @Async("githubTaskExecutor")
  public CompletableFuture<Void> saveAllRepositoriesToMongoAsync(Long userId) {
    log.info("비동기 GitHub 레포지토리 저장 시작: 사용자 ID {}", userId);
    try {
      List<Map<String, Object>> repositories = getUserRepositories(userId);
      for (Map<String, Object> repo : repositories) {
        getRepositoryDetail(userId, repo.get("fullName").toString());
      }
      log.info("비동기 GitHub 레포지토리 저장 완료: 사용자 ID {}", userId);
      return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
      log.error("비동기 GitHub 레포지토리 저장 중 오류 발생: 사용자 ID {}", userId, e);
      CompletableFuture<Void> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }
  /**
   * 레포지토리의 커밋 목록을 가져옵니다.
   */
  public List<Map<String, Object>> getRepositoryCommits(Long userId, String repoName, int maxCount) {
    try {
      GitHub github = cacheService.getGitHubConnection(userId);
      GitHubApi gitHubApi = cacheService.findByUserId(userId);

      String fullRepoName = gitHubApi.getGithubUsername() + "/" + repoName;
      GHRepository repo = github.getRepository(fullRepoName);

      return repo.listCommits().toList().stream()
          .limit(maxCount)
          .map(commit -> {
            Map<String, Object> commitMap = new HashMap<>();
            try {
              commitMap.put("sha", commit.getSHA1());
              commitMap.put("message", commit.getCommitShortInfo().getMessage());
              commitMap.put("author", commit.getCommitShortInfo().getAuthor().getName());
              commitMap.put("authorEmail", commit.getCommitShortInfo().getAuthor().getEmail());
              commitMap.put("date", commit.getCommitShortInfo().getAuthoredDate());
              commitMap.put("url", commit.getHtmlUrl().toString());
            } catch (IOException e) {
              log.warn("커밋 정보 추출 중 오류", e);
            }
            return commitMap;
          })
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.error("GitHub 커밋 내역 조회 중 오류 발생", e);
      throw new RuntimeException("GitHub 커밋 내역을 가져오는데 실패했습니다: " + repoName, e);
    }
  }

  /**
   * 레포지토리 기여자 통계를 가져옵니다.
   */
  public Map<String, Object> getRepositoryStats(Long userId, String repoName) {
    try {
      GitHub github = cacheService.getGitHubConnection(userId);
      GitHubApi gitHubApi = cacheService.findByUserId(userId);

      String fullRepoName = gitHubApi.getGithubUsername() + "/" + repoName;
      GHRepository repo = github.getRepository(fullRepoName);

      Map<String, Object> stats = new HashMap<>();
      stats.put("stars", repo.getStargazersCount());
      stats.put("watchers", repo.getWatchersCount());
      stats.put("forks", repo.getForksCount());
      stats.put("openIssues", repo.getOpenIssueCount());
      stats.put("size", repo.getSize());

      // 언어 통계
      try {
        stats.put("languages", repo.listLanguages());
      } catch (Exception e) {
        log.warn("언어 통계 조회 실패", e);
      }

      return stats;
    } catch (IOException e) {
      log.error("GitHub 레포지토리 통계 정보 조회 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 통계를 가져오는데 실패했습니다: " + repoName, e);
    }
  }
}