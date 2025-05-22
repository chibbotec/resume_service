package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import com.ll.resumeservice.domain.portfolio.github.dto.info.DownloadTaskInfo;
import com.ll.resumeservice.domain.portfolio.github.dto.request.SaveRepositoryRequest;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepoTaskStatusResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubRepositoryRepository;
import jakarta.annotation.PostConstruct;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubRepoService {

  @Value("${files.storage.path}")
  private String storageBasePath;
  private final GitHubApiService gitHubApiService;
  private final GitHubRepositoryRepository gitHubRepositoryRepository;

  // 작업 상태를 추적하는 맵
  private final Map<String, DownloadTaskInfo> taskProgressMap = new ConcurrentHashMap<>();

  // 다운로드 작업을 처리할 스레드 풀
  private ExecutorService downloadExecutor;

  @PostConstruct
  public void init() {
    // 코어 수에 맞는 스레드 풀 생성
    int processors = Runtime.getRuntime().availableProcessors();
    downloadExecutor = Executors.newFixedThreadPool(processors);

    // 주기적으로 완료된 오래된 작업 정리 (옵션)
    Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
        this::cleanupCompletedTasks, 1, 1, TimeUnit.HOURS);
  }

  private void cleanupCompletedTasks() {
    long threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
    taskProgressMap.entrySet().removeIf(entry ->
        entry.getValue().isCompleted() &&
            entry.getValue().getCompletionTime() != null &&
            entry.getValue().getCompletionTime() < threshold);
  }

  public String startAsyncRepositoryDownload(Long spaceId, Long userId,
      SaveRepositoryRequest request) {
    // 작업 ID 생성
    String taskId = UUID.randomUUID().toString();
    log.info("[Task: {}] 비동기 레포지토리 다운로드 작업 시작 - 사용자: {}, 레포지토리: {}",
        taskId, userId, request.getRepository());

    // 작업 정보 생성 및 저장
    DownloadTaskInfo taskInfo = new DownloadTaskInfo();
    taskProgressMap.put(taskId, taskInfo);

    // 백그라운드에서 작업 실행
    CompletableFuture.runAsync(() -> {
      try {
        log.info("[Task: {}] 레포지토리 다운로드 작업 시작 - 파일 수: {}",
            taskId, request.getFilePaths().size());
        RepositorySaveResponse result = doSaveRepositoryContents(taskInfo, spaceId, userId,
            request);
        taskInfo.setResult(result);
        taskInfo.setCompleted(true);
        taskInfo.setCompletionTime(System.currentTimeMillis());
        log.info("[Task: {}] 레포지토리 다운로드 작업 완료 - 저장된 파일: {}, 실패한 파일: {}",
            taskId, result.getSavedFiles().size(), result.getFailedFiles().size());
      } catch (Exception e) {
        log.error("[Task: {}] 레포지토리 다운로드 작업 실패", taskId, e);
        taskInfo.setError(e.getMessage());
        taskInfo.setCompleted(true);
        taskInfo.setCompletionTime(System.currentTimeMillis());
      }
    }, downloadExecutor).exceptionally(throwable -> {
      log.error("[Task: {}] 레포지토리 다운로드 작업 중 예외 발생", taskId, throwable);
      taskInfo.setError(throwable.getMessage());
      taskInfo.setCompleted(true);
      taskInfo.setCompletionTime(System.currentTimeMillis());
      return null;
    });

    return taskId;
  }

  public RepoTaskStatusResponse getTaskStatus(String taskId) {
    DownloadTaskInfo taskInfo = taskProgressMap.get(taskId);

    if (taskInfo == null) {
      log.warn("[Task: {}] 존재하지 않는 작업 ID", taskId);
      return null;
    }

    RepoTaskStatusResponse.RepoTaskStatusResponseBuilder builder = RepoTaskStatusResponse.builder()
        .taskId(taskId)
        .completed(taskInfo.isCompleted())
        .progress(taskInfo.getProgressPercentage())
        .totalFiles(taskInfo.getTotalFiles().get())
        .completedFiles(taskInfo.getCompletedFiles().get());

    // 완료된 경우에만 모든 정보 포함
    if (taskInfo.isCompleted()) {
      builder.savedFiles(new ArrayList<>(taskInfo.getSavedFiles()))
          .failedFiles(new ArrayList<>(taskInfo.getFailedFiles()))
          .error(taskInfo.getError());

      if (taskInfo.getResult() != null) {
        builder.savedPath(taskInfo.getResult().getSavedPath());
      }

      log.info("[Task: {}] 작업 상태 조회 - 완료됨 (진행률: {}%, 저장된 파일: {}, 실패한 파일: {})",
          taskId, taskInfo.getProgressPercentage(),
          taskInfo.getSavedFiles().size(),
          taskInfo.getFailedFiles().size());
    } else {
      log.info("[Task: {}] 작업 상태 조회 - 진행 중 (진행률: {}%, 완료된 파일: {}/{})",
          taskId, taskInfo.getProgressPercentage(),
          taskInfo.getCompletedFiles().get(),
          taskInfo.getTotalFiles().get());
    }

    return builder.build();
  }

  private RepositorySaveResponse doSaveRepositoryContents(DownloadTaskInfo taskInfo, Long spaceId,
      Long userId, SaveRepositoryRequest request) {
    try {
      // 1. GitHub 연결 및 기본 설정
      log.info("[Task] GitHub 연결 시도 - 사용자: {}", userId);
      GitHub github = gitHubApiService.getGitHubConnection(userId);
      GHRepository repo = github.getRepository(request.getRepository());
      log.info("[Task] GitHub 레포지토리 연결 성공: {}", request.getRepository());

      String repoName =
          String.format("%d_%d_", spaceId, userId) + request.getRepository().replace("/", "-");
      String saveDirectoryPath = Paths.get(storageBasePath, repoName).toString();
      log.info("[Task] 저장 경로 설정: {}", saveDirectoryPath);

      Files.createDirectories(Paths.get(saveDirectoryPath));

      // 2. 스레드 풀 생성
      int processors = Runtime.getRuntime().availableProcessors();
      ExecutorService executor = Executors.newFixedThreadPool(processors);
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      // 3. 초기 총 파일 수 설정
      taskInfo.addToTotal(request.getFilePaths().size());
      log.info("[Task] 총 파일 수 설정: {}", request.getFilePaths().size());

      // 4. 각 파일 경로에 대해 비동기 다운로드 작업 생성
      for (String filePath : request.getFilePaths()) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          try {
            // 파일 확장자가 있는 경우에만 파일로 처리
            if (filePath.contains(".")) {
              log.debug("[Task] 파일 다운로드 시작: {}", filePath);
              Path localFilePath = Paths.get(saveDirectoryPath, filePath);
              Files.createDirectories(localFilePath.getParent());

              // GitHub에서 파일 내용 가져오기
              String branch =
                  request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();
              GHContent content = repo.getFileContent(filePath, branch);

              try (InputStream is = content.read();
                  OutputStream os = new FileOutputStream(localFilePath.toString())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                  os.write(buffer, 0, bytesRead);
                }
              }

              taskInfo.getSavedFiles().add(filePath);
              log.debug("[Task] 파일 다운로드 완료: {}", filePath);
            } else {
              // 디렉토리인 경우 디렉토리만 생성
              log.debug("[Task] 디렉토리 생성: {}", filePath);
              Path dirPath = Paths.get(saveDirectoryPath, filePath);
              Files.createDirectories(dirPath);
            }

            taskInfo.incrementCompleted();
            log.debug("[Task] 진행 상황 업데이트: {}/{} ({}%)",
                taskInfo.getCompletedFiles().get(),
                taskInfo.getTotalFiles().get(),
                taskInfo.getProgressPercentage());
          } catch (Exception e) {
            taskInfo.getFailedFiles().add(filePath);
            taskInfo.incrementCompleted();
            log.error("[Task] 처리 실패: {} - {}", filePath, e.getMessage(), e);
          }
        }, executor);

        futures.add(future);
      }

      // 5. 모든 작업 완료 대기
      log.info("[Task] 모든 파일 다운로드 작업 시작 - 대기 중...");
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info("[Task] 모든 파일 다운로드 작업 완료");

      // 안전하게 스레드 풀 종료
      executor.shutdown();
      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          log.warn("[Task] 스레드 풀 강제 종료");
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        log.error("[Task] 스레드 풀 종료 중 인터럽트 발생", e);
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }

      // 6. 작업 완료 상태 설정
      taskInfo.setCompleted(true);
      taskInfo.setCompletionTime(System.currentTimeMillis());
      log.info("[Task] 작업 완료 상태 설정됨");

      // 7. 결과 반환
      log.info("[Task] GitHub 레포지토리 파일 저장 완료 - 저장된 파일: {}, 실패한 파일: {}",
          taskInfo.getSavedFiles().size(), taskInfo.getFailedFiles().size());

      return RepositorySaveResponse.builder()
          .success(true)
          .savedFiles(new ArrayList<>(taskInfo.getSavedFiles()))
          .failedFiles(new ArrayList<>(taskInfo.getFailedFiles()))
          .savedPath(saveDirectoryPath)
          .build();

    } catch (Exception e) {
      log.error("[Task] GitHub 레포지토리 정보 저장 중 오류 발생", e);
      taskInfo.setCompleted(true);
      taskInfo.setError(e.getMessage());
      taskInfo.setCompletionTime(System.currentTimeMillis());
      return RepositorySaveResponse.builder()
          .success(false)
          .failedFiles(List.of("전체 작업 실패"))
          .build();
    }
  }

  public void saveRepositoryList(Long userId) {
    try {
      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);
      List<JsonNode> repositories = getRepositoryList(userId);

      // GitHub 인스턴스 생성
      GitHub github = new GitHubBuilder()
          .withOAuthToken(gitHubApi.getGithubAccessToken())
          .build();

      for (JsonNode repo : repositories) {
        String fullName = repo.path("nameWithOwner").asText();
        String defaultBranch = repo.path("defaultBranchRef").path("name").asText("main");

        // 트리 구조 가져오기
        List<Map<String, Object>> files = new ArrayList<>();

        try {
          if (defaultBranch != null) {
            try {
              GHRepository ghRepo = github.getRepository(fullName);
              GHTree tree = ghRepo.getTreeRecursive(defaultBranch, 1);

              if (tree != null) {
                for (GHTreeEntry entry : tree.getTree()) {
                  Map<String, Object> fileInfo = new HashMap<>();
                  fileInfo.put("path", entry.getPath());
                  fileInfo.put("type", entry.getType());
                  fileInfo.put("sha", entry.getSha());
                  fileInfo.put("mode", entry.getMode());
                  fileInfo.put("size", entry.getSize());
                  files.add(fileInfo);
                }
              }
            } catch (HttpException e) {
              log.warn("레포지토리 {} 트리 정보를 가져올 수 없습니다: {}", fullName, e.getMessage());
            }
          }
        } catch (IOException treeException) {
          log.error("레포지토리 {} 트리 정보 처리 중 오류 발생", fullName, treeException);
        }

        // ✅ 기존 레포지토리 조회 (userId + fullName으로 유니크 식별)
        GitHubRepository existingRepo = gitHubRepositoryRepository
            .findByUserIdAndFullName(userId, fullName)
            .orElse(null);

        GitHubRepository gitHubRepository;

        if (existingRepo != null) {
          // ✅ 기존 레포지토리 업데이트
          log.debug("기존 레포지토리 업데이트: {}", fullName);
          existingRepo.setName(repo.path("name").asText());
          existingRepo.setDescription(repo.path("description").asText(null));
          existingRepo.setUrl(repo.path("url").asText());
          existingRepo.setSshUrl(repo.path("sshUrl").asText(null));
          existingRepo.setHomepage(repo.path("homepageUrl").asText(null));
          existingRepo.setLanguage(repo.path("primaryLanguage").path("name").asText(null));
          existingRepo.setDefaultBranch(defaultBranch);
          existingRepo.setIsPrivate(repo.path("isPrivate").asBoolean(false));
          existingRepo.setIsFork(repo.path("isFork").asBoolean(false));
          existingRepo.setIsArchived(repo.path("isArchived").asBoolean(false));
          existingRepo.setIsDisabled(repo.path("isDisabled").asBoolean(false));
          existingRepo.setStars(repo.path("stargazerCount").asInt(0));
          existingRepo.setWatchers(repo.path("watchers").path("totalCount").asInt(0));
          existingRepo.setForks(repo.path("forkCount").asInt(0));
          existingRepo.setSize(repo.path("diskUsage").asInt(0));
          existingRepo.setUpdatedAt(gitHubApiService.parseDate(repo.path("updatedAt").asText()));
          existingRepo.setPushedAt(gitHubApiService.parseDate(repo.path("pushedAt").asText()));
          existingRepo.setCommitSha(repo.path("defaultBranchRef").path("target").path("oid").asText(null));
          existingRepo.setFiles(files); // 파일 정보 업데이트
          existingRepo.setSavedAt(new Date()); // 마지막 동기화 시간 업데이트

          gitHubRepository = existingRepo;
        } else {
          // ✅ 새로운 레포지토리 생성
          log.debug("새로운 레포지토리 생성: {}", fullName);
          gitHubRepository = GitHubRepository.builder()
              .userId(userId)
              .repoId(repo.path("id").asText().hashCode() & 0x7fffffffL)
              .name(repo.path("name").asText())
              .fullName(fullName)
              .description(repo.path("description").asText(null))
              .url(repo.path("url").asText())
              .sshUrl(repo.path("sshUrl").asText(null))
              .homepage(repo.path("homepageUrl").asText(null))
              .language(repo.path("primaryLanguage").path("name").asText(null))
              .defaultBranch(defaultBranch)
              .isPrivate(repo.path("isPrivate").asBoolean(false))
              .isFork(repo.path("isFork").asBoolean(false))
              .isArchived(repo.path("isArchived").asBoolean(false))
              .isDisabled(repo.path("isDisabled").asBoolean(false))
              .stars(repo.path("stargazerCount").asInt(0))
              .watchers(repo.path("watchers").path("totalCount").asInt(0))
              .forks(repo.path("forkCount").asInt(0))
              .size(repo.path("diskUsage").asInt(0))
              .createdAt(gitHubApiService.parseDate(repo.path("createdAt").asText()))
              .updatedAt(gitHubApiService.parseDate(repo.path("updatedAt").asText()))
              .pushedAt(gitHubApiService.parseDate(repo.path("pushedAt").asText()))
              .commitSha(repo.path("defaultBranchRef").path("target").path("oid").asText(null))
              .files(files)
              .savedAt(new Date())
              .build();
        }

        // ✅ 개별 저장 (Upsert)
        gitHubRepositoryRepository.save(gitHubRepository);
      }

      log.info("GitHub 레포지토리 정보 동기화 완료 (사용자 ID: {}, 레포지토리 수: {})",
          userId, repositories.size());

    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보 저장 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

  public List<JsonNode> getRepositoryList(Long userId) {
    try {
      GitHubApi gitHubApi = gitHubApiService.findByUserId(userId);

      String query = gitHubApiService.loadGraphQLQuery("user-repositories.graphql");

      // Map으로 변수 전달
      Map<String, Object> variables = Map.of(
          "login", gitHubApi.getGithubUsername()
      );

      JsonNode response = gitHubApiService.executeGraphQLQuery(gitHubApi, query, variables);

      // 두 노드를 동시에 처리하여 하나의 List로 만들기
      List<JsonNode> allRepositories = new ArrayList<>();

      // repositories 노드 처리
      JsonNode repositories = response.path("data").path("user").path("repositories").path("nodes");
      if (repositories.isArray()) {
        repositories.forEach(allRepositories::add);
      }

      // repositoriesContributedTo 노드 처리
      JsonNode repositoriesContributedTo = response.path("data").path("user")
          .path("repositoriesContributedTo").path("nodes");
      if (repositoriesContributedTo.isArray()) {
        repositoriesContributedTo.forEach(allRepositories::add);
      }

      return allRepositories;

    } catch (Exception e) {
      log.error("GraphQL API를 통한 GitHub 레포지토리 호출 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 불러오는데 실패했습니다", e);
    }
  }
}
