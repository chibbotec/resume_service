//package com.ll.resumeservice.domain.portfolio.github.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.ll.resumeservice.domain.portfolio.github.dto.info.CommitDetailInfo;
//import com.ll.resumeservice.domain.portfolio.github.dto.info.CommitInfo;
//import com.ll.resumeservice.domain.portfolio.github.dto.info.CommitTaskInfo;
//import com.ll.resumeservice.domain.portfolio.github.dto.info.FileChangeInfo;
//import com.ll.resumeservice.domain.portfolio.github.dto.response.CommitTaskStatusResponse;
//import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class GitHubCommitServiceCopy {
//
//  private final WebClient webClient;
//  private final GitHubApiService gitHubApiService;
//  private final ExecutorService commitExecutor = Executors.newFixedThreadPool(5);
//  private final Map<String, CommitTaskInfo> taskProgressMap = new ConcurrentHashMap<>();
//
//  public List<CommitInfo> getCommitSummary(Long userId, String repoOwner, String repoName) {
//    try {
//      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
//
//      String query = gitHubApiService.loadGraphQLQuery("user-commit-history.graphql");
//
//      // Map으로 변수 전달
//      Map<String, Object> variables = Map.of(
//          "owner", repoOwner,
//          "name", repoName,
//          "author", gitHubApi.getEmail() != null
//              ? gitHubApi.getEmail()
//              : gitHubApi.getGithubUsername() + "@users.noreply.github.com"
//      );
//
//      JsonNode response = gitHubApiService.executeGraphQLQuery(gitHubApi, query, variables);
//
//      JsonNode refs = response.path("data").path("repository").path("refs").path("nodes");
//
//      List<CommitInfo> allCommits = new ArrayList<>();
//
//      for (JsonNode ref : refs) {
//        String branchName = ref.path("name").asText();
//        JsonNode commits = ref.path("target").path("history").path("nodes");
//
//
//        for (JsonNode commit : commits) {
//          CommitInfo commitInfo = CommitInfo.builder()
//              .branch(branchName)
//              .oid(commit.path("oid").asText())
//              .message(commit.path("message").asText())
//              .committedDate(gitHubApiService.parseDate(commit.path("committedDate").asText()))
//              .url(commit.path("url").asText())
//              .authorName(commit.path("author").path("name").asText())
//              .authorEmail(commit.path("author").path("email").asText())
//              .additions(commit.path("additions").asInt())
//              .deletions(commit.path("deletions").asInt())
//              .build();
//
//          allCommits.add(commitInfo);
//        }
//      }
//      return allCommits;
//
//    } catch (Exception e) {
//      log.error("GraphQL API를 통한 GitHub 커밋 호출 중 오류 발생", e);
//      throw new RuntimeException("GitHub 커밋 정보를 불러오는데 실패했습니다", e);
//    }
//  }
//
//  // 비동기 커밋 상세 정보 수집 시작
//  public String startAsyncCommitDetailCollection(Long userId, String repoOwner, String repoName,
//      List<CommitInfo> commits) {
//    String taskId = UUID.randomUUID().toString();
//    log.info("[Task: {}] 비동기 커밋 상세 정보 수집 시작 - 커밋 수: {}", taskId, commits.size());
//
//    CommitTaskInfo taskInfo = new CommitTaskInfo();
//    taskInfo.setTotalCommits(commits.size());
//    taskProgressMap.put(taskId, taskInfo);
//
//    CompletableFuture.runAsync(() -> {
//      try {
//        log.info("[Task: {}] 커밋 상세 정보 수집 작업 시작", taskId);
//        List<CommitDetailInfo> results = doCollectCommitDetails(taskInfo, userId, repoOwner,
//            repoName, commits);
//        taskInfo.setResults(results);
//        taskInfo.setCompleted(true);
//        taskInfo.setCompletionTime(System.currentTimeMillis());
//        log.info("[Task: {}] 커밋 상세 정보 수집 완료 - 성공: {}, 실패: {}",
//            taskId, taskInfo.getSuccessCount(), taskInfo.getFailedCount());
//      } catch (Exception e) {
//        log.error("[Task: {}] 커밋 상세 정보 수집 실패", taskId, e);
//        taskInfo.setError(e.getMessage());
//        taskInfo.setCompleted(true);
//        taskInfo.setCompletionTime(System.currentTimeMillis());
//      }
//    }, commitExecutor);
//
//    return taskId;
//  }
//
//  // 실제 커밋 상세 정보 수집 로직
//  private List<CommitDetailInfo> doCollectCommitDetails(
//      CommitTaskInfo taskInfo, Long userId, String repoOwner, String repoName,
//      List<CommitInfo> commits) {
//
//    try {
//      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
//      String authToken = gitHubApi.getGithubAccessToken();
//
//      List<CompletableFuture<CommitDetailInfo>> futures = new ArrayList<>();
//
//      // 각 커밋에 대해 병렬로 상세 정보 요청
//      for (CommitInfo commit : commits) {
//        CompletableFuture<CommitDetailInfo> future = CompletableFuture.supplyAsync(() -> {
//          try {
//            // REST API로 커밋 상세 정보 요청
//            String url = String.format("https://api.github.com/repos/%s/%s/commits/%s",
//                repoOwner, repoName, commit.getOid());
//
//            JsonNode response = webClient.get()
//                .uri(url)
//                .header("Authorization", "Bearer " + authToken)
//                .header("Accept", "application/vnd.github.v3+json")
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .block();
//
//            // 응답 파싱
//            CommitDetailInfo detailInfo = parseCommitDetail(commit, response);
//            taskInfo.incrementSuccess();
//
//            log.debug("[Task] 커밋 상세 정보 수집 성공: {}", commit.getOid());
//            return detailInfo;
//
//          } catch (Exception e) {
//            log.error("[Task] 커밋 상세 정보 수집 실패: {} - {}", commit.getOid(), e.getMessage());
//            taskInfo.incrementFailed();
//            taskInfo.addFailedCommit(commit.getOid());
//            return null;
//          }
//        }, commitExecutor);
//
//        futures.add(future);
//      }
//
//      // 모든 요청 완료 대기
//      List<CommitDetailInfo> results = futures.stream()
//          .map(CompletableFuture::join)
//          .filter(Objects::nonNull)
//          .collect(Collectors.toList());
//
//      return results;
//
//    } catch (Exception e) {
//      log.error("[Task] 커밋 상세 정보 수집 중 오류 발생", e);
//      throw new RuntimeException("커밋 상세 정보 수집 실패", e);
//    }
//  }
//
//  // 커밋 상세 정보 파싱
//  private CommitDetailInfo parseCommitDetail(CommitInfo basicInfo, JsonNode response) {
//    CommitDetailInfo detail = CommitDetailInfo.builder()
//        .branch(basicInfo.getBranch())
//        .sha(response.path("sha").asText())
//        .nodeId(response.path("node_id").asText())
//        .message(response.path("commit").path("message").asText())
//        .author(basicInfo.getAuthorName())
//        .email(basicInfo.getAuthorEmail())
//        .date(basicInfo.getCommittedDate())
//        .url(response.path("html_url").asText())
//        .totalChanges(response.path("stats").path("total").asInt())
//        .additions(response.path("stats").path("additions").asInt())
//        .deletions(response.path("stats").path("deletions").asInt())
//        .files(parseFiles(response.path("files")))
//        .build();
//
//    return detail;
//  }
//
//  // 파일 변경 정보 파싱
//  private List<FileChangeInfo> parseFiles(JsonNode filesNode) {
//    List<FileChangeInfo> files = new ArrayList<>();
//
//    if (filesNode.isArray()) {
//      for (JsonNode file : filesNode) {
//        FileChangeInfo fileInfo = FileChangeInfo.builder()
//            .sha(file.path("sha").asText())
//            .filename(file.path("filename").asText())
//            .status(file.path("status").asText())
//            .additions(file.path("additions").asInt())
//            .deletions(file.path("deletions").asInt())
//            .changes(file.path("changes").asInt())
//            .patch(file.path("patch").asText())
//            .blobUrl(file.path("blob_url").asText())
//            .build();
//
//        files.add(fileInfo);
//      }
//    }
//
//    return files;
//  }
//
//  // 작업 상태 조회
//  public CommitTaskStatusResponse getTaskStatus(String taskId) {
//    CommitTaskInfo taskInfo = taskProgressMap.get(taskId);
//
//    if (taskInfo == null) {
//      return null;
//    }
//
//    return CommitTaskStatusResponse.builder()
//        .taskId(taskId)
//        .completed(taskInfo.isCompleted())
//        .progress(taskInfo.getProgressPercentage())
//        .totalCommits(taskInfo.getTotalCommits())
//        .successCount(taskInfo.getSuccessCount().get())
//        .failedCount(taskInfo.getFailedCount().get())
//        .failedCommits(new ArrayList<>(taskInfo.getFailedCommits()))
//        .error(taskInfo.getError())
//        .build();
//  }
//}
