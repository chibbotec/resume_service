package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import com.ll.resumeservice.domain.portfolio.github.dto.SaveRepositoryContents;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubRepositoryRepository;
import jakarta.annotation.PostConstruct;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubRepositoryService {

  @Value("${files.storage.path}")
  private String storageBasePath;

  private final GitHubCacheService cacheService;
  private final GitHubRepositoryRepository gitHubRepositoryRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();
  private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

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

  /**
   * 오래된 완료 작업 정리
   */
  private void cleanupCompletedTasks() {
    long threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
    taskProgressMap.entrySet().removeIf(entry ->
        entry.getValue().isCompleted() &&
            entry.getValue().getCompletionTime() != null &&
            entry.getValue().getCompletionTime() < threshold);
  }

  /**
   * 비동기 레포지토리 다운로드 시작
   */
  public String startAsyncRepositoryDownload(Long spaceId, Long userId, SaveRepositoryContents request) {
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
        RepositorySaveResponse result = doSaveRepositoryContents(taskInfo, spaceId, userId, request);
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

  /**
   * 작업 진행 상황 조회
   */
  public TaskStatusResponse getTaskStatus(String taskId) {
    DownloadTaskInfo taskInfo = taskProgressMap.get(taskId);

    if (taskInfo == null) {
      log.warn("[Task: {}] 존재하지 않는 작업 ID", taskId);
      return null;
    }

    TaskStatusResponse.TaskStatusResponseBuilder builder = TaskStatusResponse.builder()
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

  /**
   * 기존 레포지토리 저장 메서드를 호출하는 메서드
   */
//  public RepositorySaveResponse saveRepositoryContents(Long spaceId, Long userId, SaveRepositoryContents request) {
//    // 동기 호출을 위한 작업 정보 생성
//    DownloadTaskInfo taskInfo = new DownloadTaskInfo();
//
//    // 실제 다운로드 로직 호출
//    return doSaveRepositoryContents(taskInfo, spaceId, userId, request);
//  }

  /**
   * 실제 레포지토리 콘텐츠 저장 로직 (기존 코드 수정)
   */
  private RepositorySaveResponse doSaveRepositoryContents(DownloadTaskInfo taskInfo, Long spaceId, Long userId, SaveRepositoryContents request) {
    try {
      // 1. GitHub 연결 및 기본 설정
      log.info("[Task] GitHub 연결 시도 - 사용자: {}", userId);
      GitHub github = cacheService.getGitHubConnection(userId);
      GHRepository repo = github.getRepository(request.getRepository());
      log.info("[Task] GitHub 레포지토리 연결 성공: {}", request.getRepository());

      String repoName = String.format("%d_%d_", spaceId, userId) + request.getRepository().replace("/", "-");
      String saveDirectoryPath = Paths.get(storageBasePath, repoName).toString();
      log.info("[Task] 저장 경로 설정: {}", saveDirectoryPath);

      Files.createDirectories(Paths.get(saveDirectoryPath));

      // 2. 스레드 풀 생성
      int processors = Runtime.getRuntime().availableProcessors();
      ExecutorService executor = Executors.newFixedThreadPool(processors);
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      // 3. 트리 구조 가져오기
      String branch = request.getBranch() != null ? request.getBranch() : repo.getDefaultBranch();
      log.info("[Task] 트리 구조 가져오기 시작 - 브랜치: {}", branch);
      
      GHTree tree = repo.getTreeRecursive(branch, 1);
      Map<String, GHTreeEntry> treeEntries = new HashMap<>();
      
      // 요청된 경로에 해당하는 트리 엔트리만 필터링
      for (GHTreeEntry entry : tree.getTree()) {
        String path = entry.getPath();
        if (request.getFilePaths().stream().anyMatch(requestPath -> 
            path.equals(requestPath) || path.startsWith(requestPath + "/"))) {
          treeEntries.put(path, entry);
        }
      }

      // 초기 총 파일 수 설정
      taskInfo.addToTotal(treeEntries.size());
      log.info("[Task] 총 파일 수 설정: {}", treeEntries.size());

      // 4. 비동기 다운로드 작업 생성
      for (Map.Entry<String, GHTreeEntry> entry : treeEntries.entrySet()) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
          try {
            String path = entry.getKey();
            GHTreeEntry treeEntry = entry.getValue();
            
            if (treeEntry.getType().equals("tree")) {
              // 디렉토리는 건너뛰기
              log.debug("[Task] 디렉토리 건너뛰기: {}", path);
              taskInfo.incrementCompleted();
            } else {
              // 파일 다운로드
              log.debug("[Task] 파일 다운로드 시작: {}", path);
              Path localFilePath = Paths.get(saveDirectoryPath, path);
              Files.createDirectories(localFilePath.getParent());
              
              // GitHub에서 파일 내용 가져오기
              GHContent content = repo.getFileContent(path, branch);
              try (InputStream is = content.read();
                   OutputStream os = new FileOutputStream(localFilePath.toString())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                  os.write(buffer, 0, bytesRead);
                }
              }
              
              taskInfo.getSavedFiles().add(path);
              taskInfo.incrementCompleted();
              log.debug("[Task] 파일 다운로드 완료: {}", path);
            }
            
            log.debug("[Task] 진행 상황 업데이트: {}/{} ({}%)", 
                taskInfo.getCompletedFiles().get(), 
                taskInfo.getTotalFiles().get(),
                taskInfo.getProgressPercentage());
          } catch (Exception e) {
            taskInfo.getFailedFiles().add(entry.getKey());
            taskInfo.incrementCompleted();
            log.error("[Task] 파일 다운로드 실패: {} - {}", entry.getKey(), e.getMessage(), e);
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

  /**
   * 디렉토리 내용을 비동기적으로 다운로드 (taskInfo 추적 기능 추가)
   */
  private void downloadDirectoryAsync(GHRepository repo, String path, String localDirPath,
      String branch, DownloadTaskInfo taskInfo,
      ExecutorService executor) throws IOException {
    try {
      // 슬래시로 끝나는 경로 처리
      String dirPath = path;
      if (dirPath.endsWith("/")) {
        dirPath = dirPath.substring(0, dirPath.length() - 1);
      }
      log.info("[Task] 디렉토리 다운로드 시작: {}", dirPath);

      // GitHub에서 디렉토리 내용 가져오기
      log.debug("[Task] GitHub에서 디렉토리 내용 요청: {}", dirPath);
      List<GHContent> contents = (branch != null && !branch.isEmpty()) ?
          repo.getDirectoryContent(dirPath, branch) : repo.getDirectoryContent(dirPath);
      log.info("[Task] 디렉토리 내용 수신 완료: {} (파일 수: {})", dirPath, contents.size());

      // 총 파일 수 업데이트
      taskInfo.addToTotal(contents.size());

      // 내용이 많은 경우 또는 중첩 디렉토리인 경우 병렬 처리
      List<CompletableFuture<Void>> contentFutures = new ArrayList<>();

      for (GHContent content : contents) {
        CompletableFuture<Void> contentFuture = CompletableFuture.runAsync(() -> {
          try {
            if (content.isDirectory()) {
              // 하위 디렉토리 처리
              log.debug("[Task] 하위 디렉토리 처리 시작: {}", content.getPath());
              downloadDirectoryAsync(repo, content.getPath(), localDirPath, branch,
                  taskInfo, executor);
              log.debug("[Task] 하위 디렉토리 처리 완료: {}", content.getPath());
            } else {
              // 파일 처리
              log.debug("[Task] 디렉토리 내 파일 처리 시작: {}", content.getPath());
              downloadFileOptimized(repo, content.getPath(), localDirPath, branch);
              taskInfo.getSavedFiles().add(content.getPath());
              taskInfo.incrementCompleted(); // 진행 상황 업데이트
              log.debug("[Task] 디렉토리 내 파일 처리 완료: {}", content.getPath());
            }
          } catch (Exception e) {
            taskInfo.getFailedFiles().add(content.getPath());
            taskInfo.incrementCompleted(); // 진행 상황 업데이트
            log.error("[Task] 콘텐츠 처리 실패: {} - {}", content.getPath(), e.getMessage(), e);
          }
        }, executor);

        contentFutures.add(contentFuture);
      }

      // 현재 디렉토리의 모든 콘텐츠 작업이 완료될 때까지 대기
      log.debug("[Task] 디렉토리 내 모든 작업 완료 대기: {}", dirPath);
      CompletableFuture.allOf(contentFutures.toArray(new CompletableFuture[0])).join();
      log.info("[Task] 디렉토리 다운로드 완료: {}", dirPath);
    } catch (Exception e) {
      taskInfo.getFailedFiles().add(path);
      log.error("[Task] 디렉토리 처리 실패: {} - {}", path, e.getMessage(), e);
      throw e;
    }
  }

  public void saveRepositoryList(Long userId) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);
      List<JsonNode> repositories = getRepositoryList(userId);
      List<GitHubRepository> gitHubRepositories = new ArrayList<>();

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
            // 빈 저장소나 트리를 가져올 수 없는 경우 예외 처리
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
              // 빈 저장소나 트리를 가져올 수 없는 경우
              log.warn("레포지토리 {} 트리 정보를 가져올 수 없습니다: {}",
                  fullName, e.getMessage());
            }
          }
        } catch (IOException treeException) {
          log.error("레포지토리 {} 트리 정보 처리 중 오류 발생",
              fullName, treeException);
        }

        GitHubRepository gitHubRepository = GitHubRepository.builder()
            .userId(userId)
            .repoId(repo.path("id").asText().hashCode() & 0x7fffffffL) // ID를 Long으로 변환
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
            .createdAt(parseDate(repo.path("createdAt").asText()))
            .updatedAt(parseDate(repo.path("updatedAt").asText()))
            .pushedAt(parseDate(repo.path("pushedAt").asText()))
            .commitSha(repo.path("defaultBranchRef").path("target").path("oid").asText(null))
            .files(files) // 트리 구조 정보 추가
            .savedAt(new Date())
            .build();

        gitHubRepositories.add(gitHubRepository);
      }

      gitHubRepositoryRepository.saveAll(gitHubRepositories);
      log.info("GitHub 레포지토리 정보 저장 완료 (사용자 ID: {}, 레포지토리 수: {})",
          userId, gitHubRepositories.size());

    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보 저장 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

  public RepositorySaveResponse saveRepositoryContents(Long spaceId, Long userId, SaveRepositoryContents request) {

    try {
      GitHub github = cacheService.getGitHubConnection(userId);

      GHRepository repo = github.getRepository(request.getRepository());

      String repoName = String.format("%d_%d_",spaceId,userId)+request.getRepository().replace("/", "-");
      String saveDirectoryPath = Paths.get(storageBasePath, repoName).toString();

      Files.createDirectories(Paths.get(saveDirectoryPath));

      List<String> savedFiles = new ArrayList<>();
      List<String> failedFiles = new ArrayList<>();

      for (String filePath : request.getFilePaths()) {
        try {
          // 디렉토리인지 파일인지 확인
          if (filePath.endsWith("/") || isDirectory(repo, filePath)) {
            // 디렉토리면 재귀적으로 모든 파일 다운로드
            downloadDirectory(repo, filePath, saveDirectoryPath, request.getBranch());
            savedFiles.add(filePath + " (directory)");
          } else {
            // 파일이면 단일 파일 다운로드
            downloadFile(repo, filePath, saveDirectoryPath, request.getBranch());
            savedFiles.add(filePath);
          }
        } catch (Exception e) {
          failedFiles.add(filePath);
          System.err.println("파일 저장 실패: " + filePath + " - " + e.getMessage());
        }
      }
      RepositorySaveResponse result = RepositorySaveResponse.builder()
          .success(true)
          .savedFiles(savedFiles)
          .failedFiles(failedFiles)
          .savedPath(saveDirectoryPath)
          .build();
      return result;

    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보 저장 중 오류 발생", e);
      return null;
    }
  }

  private boolean isDirectory(GHRepository repo, String path) throws IOException {
    try {
      List<GHContent> contents = repo.getDirectoryContent(path);
      return true; // 디렉토리 내용을 가져올 수 있으면 디렉토리
    } catch (Exception e) {
      return false; // 예외가 발생하면 디렉토리가 아님 (파일일 가능성이 높음)
    }
  }

  private void downloadFile(GHRepository repo, String path, String localDirPath, String branch)
      throws IOException {
    // 파일 경로 처리
    Path localFilePath = Paths.get(localDirPath, path);

    // 필요한 디렉토리 생성
    Files.createDirectories(localFilePath.getParent());

    // GitHub에서 파일 내용 가져오기
    GHContent content = (branch != null && !branch.isEmpty()) ?
        repo.getFileContent(path, branch) : repo.getFileContent(path);

    // 파일 저장
    try (InputStream is = content.read();
        OutputStream os = new FileOutputStream(localFilePath.toString())) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    }

    System.out.println("파일 저장 완료: " + localFilePath);
  }

  private void downloadDirectory(GHRepository repo, String path, String localDirPath, String branch)
      throws IOException {
    // 슬래시로 끝나는 경로 처리
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    // GitHub에서 디렉토리 내용 가져오기
    List<GHContent> contents = (branch != null && !branch.isEmpty()) ?
        repo.getDirectoryContent(path, branch) : repo.getDirectoryContent(path);

    for (GHContent content : contents) {
      if (content.isDirectory()) {
        // 하위 디렉토리 처리
        downloadDirectory(repo, content.getPath(), localDirPath, branch);
      } else {
        // 파일 처리
        downloadFile(repo, content.getPath(), localDirPath, branch);
      }
    }

    System.out.println("디렉토리 저장 완료: " + path);
  }

  private JsonNode executeGraphQLQuery(GitHubApi gitHubApi, String query) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(gitHubApi.getGithubAccessToken());

    // 'login' 변수와 함께 요청 생성
    Map<String, Object> requestBody = Map.of(
        "query", query,
        "variables", Map.of("login", gitHubApi.getGithubUsername()) // owner 변수 사용
    );

    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

    String response = restTemplate.postForObject(GITHUB_GRAPHQL_URL, requestEntity, String.class);
    return objectMapper.readTree(response);
  }

  public String loadGraphQLQuery(String filename) throws IOException {
    return new String(
        Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("graphql/" + filename))
            .readAllBytes(),
        StandardCharsets.UTF_8
    );
  }

  private Date parseDate(String dateString) {
    if (dateString == null || dateString.isEmpty()) {
      return null;
    }
    try {
      return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateString);
    } catch (Exception e) {
      log.error("날짜 파싱 오류: {}", dateString, e);
      return null;
    }
  }

  private void downloadFileOptimized(GHRepository repo, String path, String localDirPath, String branch) throws IOException {
    // 파일 경로 처리
    Path localFilePath = Paths.get(localDirPath, path);
    log.info("[Task] 파일 다운로드 시작: {} -> {}", path, localFilePath);

    // 필요한 디렉토리 생성
    Files.createDirectories(localFilePath.getParent());
    log.debug("[Task] 디렉토리 생성 완료: {}", localFilePath.getParent());

    // 이미 존재하는 파일인지 확인 - 캐싱 효과
    if (Files.exists(localFilePath)) {
      log.info("[Task] 파일이 이미 존재함: {}", localFilePath);
      return;
    }

    // GitHub에서 파일 내용 가져오기
    log.debug("[Task] GitHub에서 파일 내용 요청: {}", path);
    GHContent content = (branch != null && !branch.isEmpty()) ?
        repo.getFileContent(path, branch) : repo.getFileContent(path);
    log.debug("[Task] GitHub 파일 내용 수신 완료: {}", path);

    // 큰 파일을 위한 버퍼 최적화
    try (InputStream is = content.read();
        OutputStream os = new FileOutputStream(localFilePath.toString())) {
      log.debug("[Task] 파일 쓰기 시작: {}", localFilePath);
      // 버퍼 크기 증가 (8KB)
      byte[] buffer = new byte[8192];
      int bytesRead;
      long totalBytesRead = 0;
      while ((bytesRead = is.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
        totalBytesRead += bytesRead;
      }
      log.info("[Task] 파일 저장 완료: {} (크기: {} bytes)", localFilePath, totalBytesRead);
    } catch (Exception e) {
      log.error("[Task] 파일 저장 중 오류 발생: {} - {}", localFilePath, e.getMessage(), e);
      throw e;
    }
  }

  public List<JsonNode> getRepositoryList(Long userId) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);

      String query = loadGraphQLQuery("user-repositories.graphql");
      JsonNode response = executeGraphQLQuery(gitHubApi, query);
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
