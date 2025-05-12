package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import com.ll.resumeservice.domain.portfolio.github.dto.SaveRepositoryContents;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepositorySaveResponse;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubRepositoryRepository;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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

  public void saveRepositoryList(Long userId) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);
      List<JsonNode> repositories = getRepositoryList(userId);

      // 현재 userId에 해당하는 모든 레포지토리 조회
      List<GitHubRepository> existingRepositories = gitHubRepositoryRepository.findByUserId(userId);

      // repoId를 키로 하는 맵으로 변환하여 조회 성능 향상
      Map<Long, GitHubRepository> existingRepoMap = new HashMap<>();
      for (GitHubRepository repo : existingRepositories) {
        existingRepoMap.put(repo.getRepoId(), repo);
      }

      List<GitHubRepository> reposToSave = new ArrayList<>();

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

        // repoId 계산
        Long repoId = repo.path("id").asText().hashCode() & 0x7fffffffL;

        // 기존 레포지토리가 있는지 확인
        GitHubRepository existingRepo = existingRepoMap.get(repoId);

        if (existingRepo != null) {
          // 기존 레포지토리 업데이트
          existingRepo.updateFrom(
              repo.path("name").asText(),
              fullName,
              repo.path("description").asText(null),
              repo.path("url").asText(),
              repo.path("sshUrl").asText(null),
              repo.path("homepageUrl").asText(null),
              repo.path("primaryLanguage").path("name").asText(null),
              defaultBranch,
              repo.path("isPrivate").asBoolean(false),
              repo.path("isFork").asBoolean(false),
              repo.path("isArchived").asBoolean(false),
              repo.path("isDisabled").asBoolean(false),
              repo.path("stargazerCount").asInt(0),
              repo.path("watchers").path("totalCount").asInt(0),
              repo.path("forkCount").asInt(0),
              repo.path("diskUsage").asInt(0),
              parseDate(repo.path("createdAt").asText()),
              parseDate(repo.path("updatedAt").asText()),
              parseDate(repo.path("pushedAt").asText()),
              repo.path("defaultBranchRef").path("target").path("oid").asText(null),
              files,
              new Date()
          );
          reposToSave.add(existingRepo);
        } else {
          // 새 레포지토리 생성 (기존 코드와 유사)
          GitHubRepository gitHubRepository = GitHubRepository.builder()
              .userId(userId)
              .repoId(repoId) // ID를 Long으로 변환
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

          reposToSave.add(gitHubRepository);
        }
      }

      gitHubRepositoryRepository.saveAll(reposToSave);
      log.info("GitHub 레포지토리 정보 저장/업데이트 완료 (사용자 ID: {}, 레포지토리 수: {})",
          userId, reposToSave.size());

    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보 저장 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

//  public void saveRepositoryList_ex(Long userId) {
//    try {
//      GitHubApi gitHubApi = cacheService.findByUserId(userId);
//      List<JsonNode> repositories = getRepositoryList(userId);
//      List<GitHubRepository> gitHubRepositories = new ArrayList<>();
//
//      // GitHub 인스턴스 생성
//      GitHub github = new GitHubBuilder()
//          .withOAuthToken(gitHubApi.getGithubAccessToken())
//          .build();
//
//      for (JsonNode repo : repositories) {
//        String fullName = repo.path("nameWithOwner").asText();
//        String defaultBranch = repo.path("defaultBranchRef").path("name").asText("main");
//
//        // 트리 구조 가져오기
//        List<Map<String, Object>> files = new ArrayList<>();
//
//        try {
//          if (defaultBranch != null) {
//            // 빈 저장소나 트리를 가져올 수 없는 경우 예외 처리
//            try {
//              GHRepository ghRepo = github.getRepository(fullName);
//              GHTree tree = ghRepo.getTreeRecursive(defaultBranch, 1);
//
//              if (tree != null) {
//                for (GHTreeEntry entry : tree.getTree()) {
//                  Map<String, Object> fileInfo = new HashMap<>();
//                  fileInfo.put("path", entry.getPath());
//                  fileInfo.put("type", entry.getType());
//                  fileInfo.put("sha", entry.getSha());
//                  fileInfo.put("mode", entry.getMode());
//                  fileInfo.put("size", entry.getSize());
//                  files.add(fileInfo);
//                }
//              }
//            } catch (HttpException e) {
//              // 빈 저장소나 트리를 가져올 수 없는 경우
//              log.warn("레포지토리 {} 트리 정보를 가져올 수 없습니다: {}",
//                  fullName, e.getMessage());
//            }
//          }
//        } catch (IOException treeException) {
//          log.error("레포지토리 {} 트리 정보 처리 중 오류 발생",
//              fullName, treeException);
//        }
//
//        GitHubRepository gitHubRepository = GitHubRepository.builder()
//            .userId(userId)
//            .repoId(repo.path("id").asText().hashCode() & 0x7fffffffL) // ID를 Long으로 변환
//            .name(repo.path("name").asText())
//            .fullName(fullName)
//            .description(repo.path("description").asText(null))
//            .url(repo.path("url").asText())
//            .sshUrl(repo.path("sshUrl").asText(null))
//            .homepage(repo.path("homepageUrl").asText(null))
//            .language(repo.path("primaryLanguage").path("name").asText(null))
//            .defaultBranch(defaultBranch)
//            .isPrivate(repo.path("isPrivate").asBoolean(false))
//            .isFork(repo.path("isFork").asBoolean(false))
//            .isArchived(repo.path("isArchived").asBoolean(false))
//            .isDisabled(repo.path("isDisabled").asBoolean(false))
//            .stars(repo.path("stargazerCount").asInt(0))
//            .watchers(repo.path("watchers").path("totalCount").asInt(0))
//            .forks(repo.path("forkCount").asInt(0))
//            .size(repo.path("diskUsage").asInt(0))
//            .createdAt(parseDate(repo.path("createdAt").asText()))
//            .updatedAt(parseDate(repo.path("updatedAt").asText()))
//            .pushedAt(parseDate(repo.path("pushedAt").asText()))
//            .commitSha(repo.path("defaultBranchRef").path("target").path("oid").asText(null))
//            .files(files) // 트리 구조 정보 추가
//            .savedAt(new Date())
//            .build();
//
//        gitHubRepositories.add(gitHubRepository);
//      }
//
//      gitHubRepositoryRepository.saveAll(gitHubRepositories);
//      log.info("GitHub 레포지토리 정보 저장 완료 (사용자 ID: {}, 레포지토리 수: {})",
//          userId, gitHubRepositories.size());
//
//    } catch (Exception e) {
//      log.error("GitHub 레포지토리 정보 저장 중 오류 발생", e);
//      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
//    }
//  }

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
}
