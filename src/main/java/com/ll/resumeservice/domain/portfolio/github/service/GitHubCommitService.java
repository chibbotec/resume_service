package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ll.resumeservice.domain.portfolio.github.document.CommitFileList;
import com.ll.resumeservice.domain.portfolio.github.dto.request.RepoCommitRequest;
import com.ll.resumeservice.domain.portfolio.github.dto.response.CommitResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.CommitResponse.CommitFiles;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.CommitFileListRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubCommitService {

  private final WebClient webClient;
  private final GitHubApiService gitHubApiService;
  private final CommitFileListRepository commitFileListRepository;

  int processors = Runtime.getRuntime().availableProcessors();
  private final ExecutorService commitExecutor = Executors.newFixedThreadPool(processors);

  public CommitResponse getCommitFiles(Long userId, RepoCommitRequest request) {
    // 1. 입력 검증
    if (request == null || request.getRepoNames() == null || request.getRepoNames().isEmpty()) {
      return CommitResponse.builder()
          .commitFiles(Collections.emptyList())
          .build();
    }

    // 2. 병렬 처리로 성능 개선
    List<CompletableFuture<CommitFiles>> futures = request.getRepoNames().stream()
        .map(repoName -> CompletableFuture.supplyAsync(() ->
            processRepository(userId, repoName), commitExecutor))
        .collect(Collectors.toList());

    // 3. 모든 작업 완료 대기 및 결과 수집
    List<CommitFiles> commitFilesList = futures.stream()
        .map(this::safeGet)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    return CommitResponse.builder()
        .commitFiles(commitFilesList)
        .build();
  }

  private CommitFiles processRepository(Long userId, String repoName) {
    try {
      // 1. 캐시된 데이터 먼저 확인
      Optional<CommitFileList> cachedFileList = commitFileListRepository
          .findByUserIdAndRepository(userId, repoName);

      if (cachedFileList.isPresent()) {
        log.debug("캐시된 데이터 사용: {}", repoName);
        return CommitFiles.builder()
            .repository(repoName)
            .commitFiles(cachedFileList.get().getFileFullNames())
            .build();
      }

      // 2. 캐시가 없으면 새로 수집
      log.info("새로운 데이터 수집 시작: {}", repoName);
      String[] repoParts = repoName.split("/");
      if (repoParts.length != 2) {
        log.error("잘못된 레포지토리 형식: {}", repoName);
        return createEmptyCommitFiles(repoName);
      }

      CommitFileList files = collectAndSaveCommitFiles(userId, repoParts[0], repoParts[1]);
      return CommitFiles.builder()
          .repository(repoName)
          .commitFiles(files.getFileFullNames())
          .build();

    } catch (Exception e) {
      log.error("레포지토리 처리 중 오류 발생: {} - {}", repoName, e.getMessage());
      return createEmptyCommitFiles(repoName);
    }
  }

  private CommitFiles createEmptyCommitFiles(String repoName) {
    return CommitFiles.builder()
        .repository(repoName)
        .commitFiles(Collections.emptyList())
        .build();
  }

  @Transactional
  public CommitFileList collectAndSaveCommitFiles(Long userId, String repoOwner, String repoName) {
    try {
      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
      String repositoryFullName = repoOwner + "/" + repoName;
      String authToken = gitHubApi.getGithubAccessToken();

      // 파일명만 저장할 Set (중복 제거를 위해)
      Set<String> fileNamesSet = ConcurrentHashMap.newKeySet();

      // GraphQL로 커밋 정보 조회하면서 파일명 수집
      fetchAndProcessCommitFiles(gitHubApi, repoOwner, repoName, fileNamesSet, authToken);

      // Set을 List로 변환하고 정렬
      List<String> sortedFileNames = fileNamesSet.stream()
          .sorted()
          .collect(Collectors.toList());

      // CommitFileList 생성 및 저장
      CommitFileList commitFileList = CommitFileList.builder()
          .userId(userId)
          .repository(repositoryFullName)
          .fileFullNames(sortedFileNames)
          .build();

      log.info("CommitFileList 수집 완료 - 총 {} 개의 파일", sortedFileNames.size());

      return commitFileListRepository.save(commitFileList);

    } catch (Exception e) {
      log.error("GitHub 커밋 파일 목록 수집 중 오류 발생", e);
      throw new RuntimeException("GitHub 커밋 파일 목록을 처리하는데 실패했습니다", e);
    }
  }

  private void fetchAndProcessCommitFiles(GitHubApi gitHubApi, String repoOwner, String repoName,
      Set<String> fileNamesSet, String authToken) throws Exception {

    String query = gitHubApiService.loadGraphQLQuery("user-commit-history.graphql");

    // author 필터링을 위한 이메일 설정
    String authorEmail = gitHubApi.getEmail() != null
        ? gitHubApi.getEmail()
        : gitHubApi.getGithubUsername() + "@users.noreply.github.com";

    log.info("Using author filter: {}", authorEmail);

    Map<String, Object> variables = Map.of(
        "owner", repoOwner,
        "name", repoName,
        "author", authorEmail
    );

    JsonNode response = gitHubApiService.executeGraphQLQuery(gitHubApi, query, variables);
    JsonNode nodes = response.path("data").path("repository").path("refs").path("nodes");

    log.info("Found {} branches", nodes.size());

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (JsonNode ref : nodes) {
      String branchName = ref.path("name").asText();
      JsonNode commits = ref.path("target").path("history").path("nodes");

      log.info("Branch: {}, Commits: {}", branchName, commits.size());

      for (JsonNode commit : commits) {
        String sha = commit.path("oid").asText();

        // 비동기로 파일명만 수집
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
          fetchCommitFilenames(authToken, repoOwner, repoName, sha, fileNamesSet);
          return null;
        }, commitExecutor);

        futures.add(future);
      }
    }

    // 모든 비동기 작업 완료 대기
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    log.info("모든 커밋 처리 완료. 총 {} 개의 고유 파일 발견", fileNamesSet.size());
  }

  private void fetchCommitFilenames(String authToken, String repoOwner, String repoName,
      String sha, Set<String> fileNamesSet) {
    try {
      // REST API로 커밋 상세 정보 조회
      String url = String.format("https://api.github.com/repos/%s/%s/commits/%s",
          repoOwner, repoName, sha);

      JsonNode detailResponse = webClient.get()
          .uri(url)
          .header("Authorization", "Bearer " + authToken)
          .header("Accept", "application/vnd.github.v3+json")
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block();

      JsonNode filesNode = detailResponse.path("files");

      if (filesNode.isArray()) {
        for (JsonNode file : filesNode) {
          String filename = file.path("filename").asText();
          // 파일명만 Set에 추가 (중복 자동 제거)
          fileNamesSet.add(filename);
        }
      }

      log.debug("커밋 {} 처리 완료 - {} 개 파일", sha, filesNode.size());

    } catch (Exception e) {
      log.error("커밋 파일명 수집 실패: {} - {}", sha, e.getMessage());
    }
  }

  // 기존에 저장된 파일 목록 조회
  public CommitFileList getCommitFileList(Long userId, String repository) {
    return commitFileListRepository.findByUserIdAndRepository(userId, repository)
        .orElse(null);
  }

  // 기존 데이터 삭제 후 새로 수집
  @Transactional
  public CommitFileList refreshCommitFiles(Long userId, String repoOwner, String repoName) {
    String repositoryFullName = repoOwner + "/" + repoName;

    // 기존 데이터 삭제
    commitFileListRepository.deleteByUserIdAndRepository(userId, repositoryFullName);

    // 새로 수집
    return collectAndSaveCommitFiles(userId, repoOwner, repoName);
  }

  private <T> T safeGet(CompletableFuture<T> future) {
    try {
      return future.get(30, TimeUnit.SECONDS); // 타임아웃 설정
    } catch (Exception e) {
      log.error("비동기 작업 처리 중 오류: {}", e.getMessage());
      return null;
    }
  }
}