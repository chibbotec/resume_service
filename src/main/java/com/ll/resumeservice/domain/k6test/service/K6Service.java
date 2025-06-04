package com.ll.resumeservice.domain.k6test.service;

import com.ll.resumeservice.domain.portfolio.github.dto.info.DownloadTaskInfo;
import com.ll.resumeservice.domain.portfolio.github.dto.request.SaveRepositoryRequest;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepoTaskStatusResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import com.ll.resumeservice.domain.portfolio.github.service.GitHubApiService;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.kohsuke.github.GitHub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class K6Service {

  @Value("${files.test.path}")
  private String storageBasePath;
  private final GitHubApiService gitHubApiService;

  // 작업 상태를 추적하는 맵
  private final Map<String, DownloadTaskInfo> taskProgressMap = new ConcurrentHashMap<>();

  int processors = Runtime.getRuntime().availableProcessors();
  private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(processors);

  private void cleanupCompletedTasks() {
    long threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
    taskProgressMap.entrySet().removeIf(entry ->
        entry.getValue().isCompleted() &&
            entry.getValue().getCompletionTime() != null &&
            entry.getValue().getCompletionTime() < threshold);
  }

  public String AsyncRepositoryDownload(Long spaceId, Long userId,
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
            taskId, null);
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
//      GitHub github = new GitHubBuilder()
//          .withOAuthToken("")
//          .build();
      GHRepository repo = github.getRepository(request.getRepository());
      log.info("[Task] GitHub 레포지토리 연결 성공: {}", request.getRepository());

      String repoName =
          String.format("%d_%d_", spaceId, userId) + request.getRepository().replace("/", "-");
      String saveDirectoryPath = Paths.get(storageBasePath, repoName).toString();
      log.info("[Task] 저장 경로 설정: {}", saveDirectoryPath);

      Files.createDirectories(Paths.get(saveDirectoryPath));

      // 2. 파일 경로 결정 - request에 파일 경로가 있으면 사용, 없으면 전체 레포 탐색
      List<String> filePaths;
      if (request.getFilePaths() != null && !request.getFilePaths().isEmpty()) {
        // 요청에 특정 파일 경로가 있는 경우
        filePaths = request.getFilePaths();
        log.info("[Task] 요청된 특정 파일들 다운로드 - 파일 수: {}", filePaths.size());
      } else {
        // 요청에 파일 경로가 없는 경우 전체 레포지토리 탐색
        String branch = request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();
        filePaths = getAllRepositoryFiles(repo, branch);
        log.info("[Task] 전체 레포지토리 탐색 완료 - 발견된 파일 수: {}", filePaths.size());
      }

      // 3. 스레드 풀 생성
      int processors = Runtime.getRuntime().availableProcessors();
      ExecutorService executor = Executors.newFixedThreadPool(processors);
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      // 4. 초기 총 파일 수 설정
      taskInfo.addToTotal(filePaths.size());
      log.info("[Task] 총 파일 수 설정: {}", filePaths.size());

      // 5. 각 파일 경로에 대해 비동기 다운로드 작업 생성
      String branch = request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();

      for (String filePath : filePaths) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          try {
            log.debug("[Task] 파일 다운로드 시작: {}", filePath);
            Path localFilePath = Paths.get(saveDirectoryPath, filePath);
            Files.createDirectories(localFilePath.getParent());

            // GitHub에서 파일 내용 가져오기
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

      // 6. 모든 작업 완료 대기
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

      // 7. 작업 완료 상태 설정
      taskInfo.setCompleted(true);
      taskInfo.setCompletionTime(System.currentTimeMillis());
      log.info("[Task] 작업 완료 상태 설정됨");

      // 8. 결과 반환
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

  public String serailRepositoryDownload(Long spaceId, Long userId,
      SaveRepositoryRequest request) {
    // 작업 ID 생성
    String taskId = UUID.randomUUID().toString();
    log.info("[Task: {}] 비동기 순차 레포지토리 다운로드 작업 시작 - 사용자: {}, 레포지토리: {}",
        taskId, userId, request.getRepository());

    // 작업 정보 생성 및 저장
    DownloadTaskInfo taskInfo = new DownloadTaskInfo();
    taskProgressMap.put(taskId, taskInfo);

    // 백그라운드에서 순차적으로 작업 실행
    CompletableFuture.runAsync(() -> {
      try {
        log.info("[Task: {}] 레포지토리 다운로드 작업 시작 - 파일 수: {}",
            taskId, null);

        // 순차 처리용 메서드 호출
        RepositorySaveResponse result = doSaveRepositoryContentsSequential(taskInfo, spaceId, userId, request);

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

  // 수정된 다운로드 메서드
  private RepositorySaveResponse doSaveRepositoryContentsSequential(DownloadTaskInfo taskInfo,
      Long spaceId, Long userId, SaveRepositoryRequest request) {
    try {
      // 1. GitHub 연결 및 기본 설정
      log.info("[Task] GitHub 연결 시도 - 사용자: {}", userId);
//      GitHub github = new GitHubBuilder()
//          .withOAuthToken("")
//          .build();
      GitHub github = gitHubApiService.getGitHubConnection(userId);
      GHRepository repo = github.getRepository(request.getRepository());
      log.info("[Task] GitHub 레포지토리 연결 성공: {}", request.getRepository());

      String repoName = String.format("%d_%d_", spaceId, userId) + request.getRepository().replace("/", "-");
      String saveDirectoryPath = Paths.get(storageBasePath, repoName).toString();
      log.info("[Task] 저장 경로 설정: {}", saveDirectoryPath);

      Files.createDirectories(Paths.get(saveDirectoryPath));

      // 2. 파일 경로 결정 - request에 파일 경로가 있으면 사용, 없으면 전체 레포 탐색
      List<String> filePaths;
      if (request.getFilePaths() != null && !request.getFilePaths().isEmpty()) {
        // 요청에 특정 파일 경로가 있는 경우
        filePaths = request.getFilePaths();
        log.info("[Task] 요청된 특정 파일들 다운로드 - 파일 수: {}", filePaths.size());
      } else {
        // 요청에 파일 경로가 없는 경우 전체 레포지토리 탐색
        String branch = request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();
        filePaths = getAllRepositoryFiles(repo, branch);
        log.info("[Task] 전체 레포지토리 탐색 완료 - 발견된 파일 수: {}", filePaths.size());
      }

      // 3. 초기 총 파일 수 설정
      taskInfo.addToTotal(filePaths.size());
      log.info("[Task] 총 파일 수 설정: {}", filePaths.size());

      // 4. 순차적으로 파일 다운로드
      String branch = request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();

      for (String filePath : filePaths) {
        try {
          log.debug("[Task] 파일 다운로드 시작: {}", filePath);
          Path localFilePath = Paths.get(saveDirectoryPath, filePath);
          Files.createDirectories(localFilePath.getParent());

          // GitHub에서 파일 내용 가져오기
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

          taskInfo.incrementCompleted();
          log.debug("[Task] 진행 상황 업데이트: {}/{} ({}%)",
              taskInfo.getCompletedFiles().get(),
              taskInfo.getTotalFiles().get(),
              taskInfo.getProgressPercentage());
        } catch (Exception e) {
          taskInfo.getFailedFiles().add(filePath);
          taskInfo.incrementCompleted();
          log.error("[Task] 파일 처리 실패: {} - {}", filePath, e.getMessage(), e);
        }
      }

      // 5. 작업 완료 상태 설정
      taskInfo.setCompleted(true);
      taskInfo.setCompletionTime(System.currentTimeMillis());
      log.info("[Task] 순차 다운로드 작업 완료");

      // 6. 결과 반환
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

  // 레포지토리 전체 구조를 가져오는 메서드
  private List<String> getAllRepositoryFiles(GHRepository repo, String branch) throws IOException {
    List<String> allFilePaths = new ArrayList<>();

    // 루트 디렉토리부터 재귀적으로 탐색
    exploreDirectory(repo, "", branch, allFilePaths);

    return allFilePaths;
  }

  // 재귀적으로 디렉토리 탐색
  private void exploreDirectory(GHRepository repo, String path, String branch, List<String> filePaths) throws IOException {
    try {
      List<GHContent> contents = repo.getDirectoryContent(path, branch);

      for (GHContent content : contents) {
        if (content.isFile()) {
          // 파일인 경우 경로 추가
          filePaths.add(content.getPath());
          log.debug("파일 발견: {}", content.getPath());
        } else if (content.isDirectory()) {
          // 디렉토리인 경우 재귀적으로 탐색
          log.debug("디렉토리 탐색: {}", content.getPath());
          exploreDirectory(repo, content.getPath(), branch, filePaths);
        }
      }
    } catch (IOException e) {
      log.error("디렉토리 탐색 실패: {} - {}", path, e.getMessage());
      throw e;
    }
  }
}
