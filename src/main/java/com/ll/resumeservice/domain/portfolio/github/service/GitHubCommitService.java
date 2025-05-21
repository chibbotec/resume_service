package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ll.resumeservice.domain.portfolio.github.document.GithubCommit;
import com.ll.resumeservice.domain.portfolio.github.document.GithubCommit.CommitDocument;
import com.ll.resumeservice.domain.portfolio.github.document.GithubCommit.FileDocument;
import com.ll.resumeservice.domain.portfolio.github.dto.info.CommitTaskInfo;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GithubCommitRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubCommitService {

  private final WebClient webClient;
  private final GitHubApiService gitHubApiService;
  private final GithubCommitRepository githubCommitRepository;
  private final ExecutorService commitExecutor = Executors.newFixedThreadPool(5);
  private final Map<String, CommitTaskInfo> taskProgressMap = new ConcurrentHashMap<>();

  @Transactional
  public GithubCommit collectAndSaveCommits(Long userId, String repoOwner, String repoName) {
    try {
      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
      String repositoryFullName = repoOwner + "/" + repoName;
      String authToken = gitHubApi.getGithubAccessToken();

      // 1. GithubCommit 문서 생성 또는 조회
      GithubCommit githubCommit = new GithubCommit();
      githubCommit.setBranches(new HashSet<>());
      githubCommit.setContributors(new HashSet<>());
      githubCommit.setCommits(new ArrayList<>());

      // 2. GraphQL로 커밋 정보 조회하면서 바로 처리
      fetchAndProcessCommits(gitHubApi, repoOwner, repoName, githubCommit, authToken);

      // 3. 통계 업데이트
      updateStatistics(githubCommit);

      // 4. 최종 저장
      return githubCommitRepository.save(githubCommit);

    } catch (Exception e) {
      log.error("GitHub 커밋 수집 및 저장 중 오류 발생", e);
      throw new RuntimeException("GitHub 커밋 정보를 처리하는데 실패했습니다", e);
    }
  }

  private void fetchAndProcessCommits(GitHubApi gitHubApi, String repoOwner, String repoName,
      GithubCommit githubCommit, String authToken) throws Exception {

    String query = gitHubApiService.loadGraphQLQuery("user-commit-history.graphql");
    Map<String, Object> variables = Map.of(
        "owner", repoOwner,
        "name", repoName,
        "author", gitHubApi.getEmail() != null
            ? gitHubApi.getEmail()
            : gitHubApi.getGithubUsername() + "@users.noreply.github.com"
    );

    JsonNode response = gitHubApiService.executeGraphQLQuery(gitHubApi, query, variables);

    // 전체 응답 로깅
    log.info("Full GraphQL response: {}", response.toPrettyString());

    // 단계별 파싱 확인
    JsonNode data = response.path("data");
    log.info("data node exists: {}", !data.isMissingNode());

    JsonNode repository = data.path("repository");
    log.info("repository node exists: {}", !repository.isMissingNode());

    JsonNode refs = repository.path("refs");
    log.info("refs node exists: {}", !refs.isMissingNode());

    JsonNode nodes = refs.path("nodes");
    log.info("refs.nodes exists: {}, size: {}", !nodes.isMissingNode(), nodes.size());

    // 초기화
    if (githubCommit.getCommits() == null) {
      githubCommit.setCommits(new ArrayList<>());
    }
    if (githubCommit.getBranches() == null) {
      githubCommit.setBranches(new HashSet<>());
    }
    if (githubCommit.getContributors() == null) {
      githubCommit.setContributors(new HashSet<>());
    }

    List<CompletableFuture<CommitDocument>> futures = new ArrayList<>();

    for (JsonNode ref : nodes) {
      String branchName = ref.path("name").asText();
      JsonNode commits = ref.path("target").path("history").path("nodes");
      log.info("commits : {}", commits);
      // 브랜치 정보 추가
      githubCommit.getBranches().add(branchName);

      for (JsonNode commit : commits) {
        String sha = commit.path("oid").asText();
        log.info("oid : {}", sha);

        // 이미 존재하는 커밋인지 체크
        boolean exists = githubCommit.getCommits().stream()
            .anyMatch(c -> c.getSha().equals(sha));

        if (!exists) {
          // 기여자 정보 추가
          String authorEmail = commit.path("author").path("email").asText();
          githubCommit.getContributors().add(authorEmail);

          // 비동기로 상세 정보 수집
          CompletableFuture<CommitDocument> future = CompletableFuture.supplyAsync(() ->
                  fetchCommitDetail(authToken, repoOwner, repoName, branchName, commit),
              commitExecutor);
          futures.add(future);
        }
      }
    }

    // 모든 비동기 작업 완료 대기 및 커밋 추가
    List<CommitDocument> newCommits = futures.stream()
        .map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    githubCommit.getCommits().addAll(newCommits);
    githubCommit.setUpdatedAt(new Date());
  }

  private CommitDocument fetchCommitDetail(String authToken, String repoOwner,
      String repoName, String branchName, JsonNode basicCommitInfo) {

    String sha = basicCommitInfo.path("oid").asText();

    try {
      // REST API로 상세 정보 조회
      String url = String.format("https://api.github.com/repos/%s/%s/commits/%s",
          repoOwner, repoName, sha);
      log.info("Fetching commit detail from URL: {}", url);

      JsonNode detailResponse = webClient.get()
          .uri(url)
          .header("Authorization", "Bearer " + authToken)
          .header("Accept", "application/vnd.github.v3+json")
          .retrieve()
          .bodyToMono(JsonNode.class)
          .block();

      // 파일 정보 파싱
      List<FileDocument> fileDocuments = parseFiles(detailResponse.path("files"));

      // CommitDocument 생성
      return CommitDocument.builder()
          .branch(branchName)
          .sha(sha)
          .nodeId(detailResponse.path("node_id").asText())
          .message(basicCommitInfo.path("message").asText())
          .author(basicCommitInfo.path("author").path("name").asText())
          .email(basicCommitInfo.path("author").path("email").asText())
          .date(gitHubApiService.parseDate(basicCommitInfo.path("committedDate").asText()))
          .url(detailResponse.path("html_url").asText())
          .totalChanges(detailResponse.path("stats").path("total").asInt())
          .additions(detailResponse.path("stats").path("additions").asInt())
          .deletions(detailResponse.path("stats").path("deletions").asInt())
          .files(fileDocuments)
          .build();

    } catch (Exception e) {
      log.error("커밋 상세 정보 수집 실패: {} - {}", sha, e.getMessage());

      // 실패 시 기본 정보만으로 생성
      return CommitDocument.builder()
          .branch(branchName)
          .sha(sha)
          .nodeId("")
          .message(basicCommitInfo.path("message").asText())
          .author(basicCommitInfo.path("author").path("name").asText())
          .email(basicCommitInfo.path("author").path("email").asText())
          .date(gitHubApiService.parseDate(basicCommitInfo.path("committedDate").asText()))
          .url(basicCommitInfo.path("url").asText())
          .totalChanges(
              basicCommitInfo.path("additions").asInt() + basicCommitInfo.path("deletions").asInt())
          .additions(basicCommitInfo.path("additions").asInt())
          .deletions(basicCommitInfo.path("deletions").asInt())
          .files(new ArrayList<>())
          .build();
    }
  }

  private void updateStatistics(GithubCommit githubCommit) {
    githubCommit.setTotalCommits(githubCommit.getCommits().size());
    githubCommit.setTotalBranches(githubCommit.getBranches().size());

    int totalAdditions = githubCommit.getCommits().stream()
        .mapToInt(CommitDocument::getAdditions)
        .sum();
    int totalDeletions = githubCommit.getCommits().stream()
        .mapToInt(CommitDocument::getDeletions)
        .sum();

    githubCommit.setTotalAdditions(totalAdditions);
    githubCommit.setTotalDeletions(totalDeletions);
    githubCommit.setUpdatedAt(new Date());
  }

  private List<FileDocument> parseFiles(JsonNode filesNode) {
    List<FileDocument> files = new ArrayList<>();

    if (filesNode.isArray()) {
      for (JsonNode file : filesNode) {
        FileDocument fileDoc = FileDocument.builder()
            .sha(file.path("sha").asText())
            .filename(file.path("filename").asText())
            .status(file.path("status").asText())
            .additions(file.path("additions").asInt())
            .deletions(file.path("deletions").asInt())
            .changes(file.path("changes").asInt())
            .patch(file.path("patch").asText())
            .blobUrl(file.path("blob_url").asText())
            .build();
        files.add(fileDoc);
      }
    }
    return files;
  }


}
