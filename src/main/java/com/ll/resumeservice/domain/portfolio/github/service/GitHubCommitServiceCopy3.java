//package com.ll.resumeservice.domain.portfolio.github.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.ll.resumeservice.domain.portfolio.github.document.CommitHistory;
//import com.ll.resumeservice.domain.portfolio.github.document.CommitHistory.FileHistory;
//import com.ll.resumeservice.domain.portfolio.github.document.CommitHistory.GroupByFiles;
//import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
//import com.ll.resumeservice.domain.portfolio.github.repository.CommitHistoryRepository;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class GitHubCommitServiceCopy3 {
//
//  private final WebClient webClient;
//  private final GitHubApiService gitHubApiService;
//  private final CommitHistoryRepository commitHistoryRepository;
//  private final ExecutorService commitExecutor = Executors.newFixedThreadPool(3);
//
//  @Transactional
//  public CommitHistory collectAndSaveCommits(Long userId, String repoOwner, String repoName) {
//    try {
//      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
//      String repositoryFullName = repoOwner + "/" + repoName;
//      String authToken = gitHubApi.getGithubAccessToken();
//
//      // 파일별로 그룹화된 맵 (동시성 처리를 위해 ConcurrentHashMap 사용)
//      Map<String, List<FileHistory>> fileGroupMap = new ConcurrentHashMap<>();
//
//      // GraphQL로 커밋 정보 조회하면서 파일별로 그룹화
//      fetchAndProcessCommits(gitHubApi, repoOwner, repoName, fileGroupMap, authToken);
//
//      // Map을 GroupByFiles 리스트로 변환
//      List<GroupByFiles> groupByFilesList = fileGroupMap.entrySet().stream()
//          .map(entry -> GroupByFiles.builder()
//              .filename(entry.getKey())
//              .fileHistories(entry.getValue().stream()
//                  // 날짜 역순으로 정렬 (최신 커밋이 먼저)
//                  .sorted((f1, f2) -> f2.getCommitDate().compareTo(f1.getCommitDate()))
//                  .collect(Collectors.toList()))
//              .build())
//          // 파일명 알파벳 순으로 정렬
//          .sorted(Comparator.comparing(GroupByFiles::getFilename))
//          .collect(Collectors.toList());
//
//      // CommitHistory 생성 및 저장
//      CommitHistory commitHistory = CommitHistory.builder()
//          .userId(userId)
//          .repository(repositoryFullName)
//          .commits(groupByFilesList)
//          .build();
//
//      log.info("CommitHistory 수집 완료 - 총 {} 개의 파일", groupByFilesList.size());
//
//      return commitHistoryRepository.save(commitHistory);
//
//    } catch (Exception e) {
//      log.error("GitHub 커밋 히스토리 수집 중 오류 발생", e);
//      throw new RuntimeException("GitHub 커밋 히스토리를 처리하는데 실패했습니다", e);
//    }
//  }
//
//  private void fetchAndProcessCommits(GitHubApi gitHubApi, String repoOwner, String repoName,
//      Map<String, List<FileHistory>> fileGroupMap, String authToken) throws Exception {
//
//    String query = gitHubApiService.loadGraphQLQuery("user-commit-history.graphql");
//
//    // author 필터링을 위한 이메일 설정
//    String authorEmail = gitHubApi.getEmail() != null
//        ? gitHubApi.getEmail()
//        : gitHubApi.getGithubUsername() + "@users.noreply.github.com";
//
//    log.info("Using author filter: {}", authorEmail);
//
//    Map<String, Object> variables = Map.of(
//        "owner", repoOwner,
//        "name", repoName,
//        "author", authorEmail
//    );
//
//    JsonNode response = gitHubApiService.executeGraphQLQuery(gitHubApi, query, variables);
//    JsonNode nodes = response.path("data").path("repository").path("refs").path("nodes");
//
//    log.info("Found {} branches", nodes.size());
//
//    List<CompletableFuture<Void>> futures = new ArrayList<>();
//
//    for (JsonNode ref : nodes) {
//      String branchName = ref.path("name").asText();
//      JsonNode commits = ref.path("target").path("history").path("nodes");
//
//      log.info("Branch: {}, Commits: {}", branchName, commits.size());
//
//      for (JsonNode commit : commits) {
//        String sha = commit.path("oid").asText();
//        Date commitDate = gitHubApiService.parseDate(commit.path("committedDate").asText());
//
//        // 비동기로 상세 정보 수집 및 파일별 그룹화
//        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
//          fetchCommitDetailAndGroup(authToken, repoOwner, repoName, branchName,
//              sha, commitDate, fileGroupMap);
//          return null;
//        }, commitExecutor);
//
//        futures.add(future);
//      }
//    }
//
//    // 모든 비동기 작업 완료 대기
//    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//    log.info("모든 커밋 처리 완료. 총 {} 개의 파일 발견", fileGroupMap.size());
//  }
//
//  private void fetchCommitDetailAndGroup(String authToken, String repoOwner, String repoName,
//      String branchName, String sha, Date commitDate,
//      Map<String, List<FileHistory>> fileGroupMap) {
//    try {
//      // REST API로 커밋 상세 정보 조회
//      String url = String.format("https://api.github.com/repos/%s/%s/commits/%s",
//          repoOwner, repoName, sha);
//
//      JsonNode detailResponse = webClient.get()
//          .uri(url)
//          .header("Authorization", "Bearer " + authToken)
//          .header("Accept", "application/vnd.github.v3+json")
//          .retrieve()
//          .bodyToMono(JsonNode.class)
//          .block();
//
//      JsonNode filesNode = detailResponse.path("files");
//
//      if (filesNode.isArray()) {
//        for (JsonNode file : filesNode) {
//          String filename = file.path("filename").asText();
//          String patch = file.path("patch").asText("");
//
//          // 파일 히스토리 생성
//          FileHistory fileHistory = FileHistory.builder()
//              .branch(branchName)
//              .commitDate(commitDate)
//              .patch(patch)
//              .build();
//
//          // 파일명을 키로 하여 히스토리 추가 (thread-safe)
//          fileGroupMap.computeIfAbsent(filename, k -> Collections.synchronizedList(new ArrayList<>()))
//              .add(fileHistory);
//        }
//      }
//
//      log.debug("커밋 {} 처리 완료 - {} 개 파일", sha, filesNode.size());
//
//    } catch (Exception e) {
//      log.error("커밋 상세 정보 수집 실패: {} - {}", sha, e.getMessage());
//    }
//  }
//
//}