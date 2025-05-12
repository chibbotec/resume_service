package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubGraphQLService {

  private final GitHubCacheService cacheService;
  private final GitHubMongoService gitHubMongoService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();
  private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

  /**
   * GraphQL API를 사용하여 레포지토리 상세 정보를 가져옵니다.
   */
  public Map<String, Object> getRepositoryDetailWithGraphQL(Long userId, String repoName) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);
      String owner = gitHubApi.getGithubUsername();
      String repo = repoName;

      // 레포지토리 이름에 owner가 포함되어 있는지 확인
      if (repoName.contains("/")) {
        String[] parts = repoName.split("/");
        owner = parts[0];
        repo = parts[1];
      }

      // GraphQL 쿼리 생성
      String query = String.format("""
                {
                  repository(owner: "%s", name: "%s") {
                    id
                    databaseId
                    name
                    nameWithOwner
                    description
                    url
                    homepageUrl
                    primaryLanguage {
                      name
                    }
                    defaultBranchRef {
                      name
                    }
                    isPrivate
                    isFork
                    isArchived
                    isDisabled
                    stargazerCount
                    watchers {
                      totalCount
                    }
                    forkCount
                    diskUsage
                    createdAt
                    updatedAt
                    pushedAt
                    object(expression: "HEAD:") {
                      ... on Tree {
                        entries {
                          name
                          type
                          path
                        }
                      }
                    }
                  }
                }
            """, owner, repo);

      // GraphQL 요청 전송
      JsonNode response = executeGraphQLQuery(gitHubApi.getGithubAccessToken(), query);
      JsonNode repoData = response.path("data").path("repository");

      if (repoData.isMissingNode() || repoData.isNull()) {
        throw new RuntimeException("레포지토리를 찾을 수 없습니다: " + repoName);
      }

      // 응답 데이터 매핑
      Map<String, Object> repoDetail = new HashMap<>();
      repoDetail.put("id", repoData.path("databaseId").asLong());
      repoDetail.put("name", repoData.path("name").asText());
      repoDetail.put("fullName", repoData.path("nameWithOwner").asText());
      repoDetail.put("description", repoData.path("description").asText(""));
      repoDetail.put("url", repoData.path("url").asText());
      repoDetail.put("homepage", repoData.path("homepageUrl").asText(""));

      // 언어 처리
      if (!repoData.path("primaryLanguage").isNull()) {
        repoDetail.put("language", repoData.path("primaryLanguage").path("name").asText());
      } else {
        repoDetail.put("language", "");
      }

      repoDetail.put("defaultBranch", repoData.path("defaultBranchRef").path("name").asText("main"));
      repoDetail.put("private", repoData.path("isPrivate").asBoolean());
      repoDetail.put("fork", repoData.path("isFork").asBoolean());
      repoDetail.put("archived", repoData.path("isArchived").asBoolean());
      repoDetail.put("disabled", repoData.path("isDisabled").asBoolean());
      repoDetail.put("stars", repoData.path("stargazerCount").asInt());
      repoDetail.put("watchers", repoData.path("watchers").path("totalCount").asInt());
      repoDetail.put("forks", repoData.path("forkCount").asInt());
      repoDetail.put("size", repoData.path("diskUsage").asInt());

      // 날짜 변환
      repoDetail.put("createdAt", parseDate(repoData.path("createdAt").asText()));
      repoDetail.put("updatedAt", parseDate(repoData.path("updatedAt").asText()));
      repoDetail.put("pushedAt", parseDate(repoData.path("pushedAt").asText()));

      // 파일 목록 추출
      JsonNode entriesNode = repoData.path("object").path("entries");
      List<Map<String, Object>> files = new ArrayList<>();

      if (!entriesNode.isMissingNode() && entriesNode.isArray()) {
        for (JsonNode entry : entriesNode) {
          Map<String, Object> file = new HashMap<>();
          file.put("name", entry.path("name").asText());
          file.put("path", entry.path("path").asText());
          file.put("type", "tree".equals(entry.path("type").asText()) ? "dir" : "file");

          // 디렉토리인 경우 별도 요청으로 내부 구조 가져오기
          if ("dir".equals(file.get("type"))) {
            file.put("directory", getDirectoryContents(gitHubApi.getGithubAccessToken(), owner, repo, entry.path("path").asText()));
          }

          files.add(file);
        }
      }

      repoDetail.put("files", files);

      // MongoDB에 저장
      gitHubMongoService.saveRepositoryToMongo(userId, repoDetail);

      return repoDetail;
    } catch (Exception e) {
      log.error("GitHub GraphQL API 호출 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 상세 정보를 가져오는데 실패했습니다: " + repoName, e);
    }
  }

  /**
   * 특정 디렉토리의 내용을 GraphQL로 가져옵니다.
   */
  private List<Map<String, Object>> getDirectoryContents(String token, String owner, String repo, String path) {
    try {
      String query = String.format("""
                {
                  repository(owner: "%s", name: "%s") {
                    object(expression: "HEAD:%s") {
                      ... on Tree {
                        entries {
                          name
                          type
                          path
                        }
                      }
                    }
                  }
                }
            """, owner, repo, path);

      JsonNode response = executeGraphQLQuery(token, query);
      JsonNode entriesNode = response.path("data").path("repository").path("object").path("entries");
      List<Map<String, Object>> contents = new ArrayList<>();

      if (!entriesNode.isMissingNode() && entriesNode.isArray()) {
        for (JsonNode entry : entriesNode) {
          Map<String, Object> item = new HashMap<>();
          item.put("name", entry.path("name").asText());
          item.put("path", entry.path("path").asText());
          item.put("type", "tree".equals(entry.path("type").asText()) ? "dir" : "file");

          // 재귀 호출을 제한 (한 단계만 더 들어감)
          // 필요에 따라 더 깊은 탐색을 위해 코드 수정 가능
          contents.add(item);
        }
      }

      return contents;
    } catch (Exception e) {
      log.warn("디렉토리 내용 조회 중 오류 발생: " + path, e);
      return new ArrayList<>();
    }
  }

  /**
   * GraphQL 쿼리를 실행하여 결과를 반환합니다.
   */
  private JsonNode executeGraphQLQuery(String token, String query) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    Map<String, String> body = Map.of("query", query);
    HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

    String response = restTemplate.postForObject(GITHUB_GRAPHQL_URL, requestEntity, String.class);
    return objectMapper.readTree(response);
  }

  /**
   * ISO 8601 형식의 날짜 문자열을 Date 객체로 변환합니다.
   */
  private Date parseDate(String dateString) {
    try {
      return new Date(java.time.Instant.parse(dateString).toEpochMilli());
    } catch (Exception e) {
      log.warn("날짜 변환 중 오류 발생: " + dateString, e);
      return new Date();
    }
  }

  /**
   * 사용자의 모든 레포지토리 정보를 GraphQL을 통해 가져와서 MongoDB에 저장합니다.
   */
  public void saveAllRepositoriesToMongoWithGraphQL(Long userId) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);
      String owner = gitHubApi.getGithubUsername();

      // 먼저 레포지토리 목록을 가져옵니다
      String query = String.format("""
                {
                  user(login: "%s") {
                    repositories(first: 100) {
                      nodes {
                        name
                        nameWithOwner
                      }
                    }
                  }
                }
            """, owner);

      JsonNode response = executeGraphQLQuery(gitHubApi.getGithubAccessToken(), query);
      JsonNode repositories = response.path("data").path("user").path("repositories").path("nodes");

      if (repositories.isArray()) {
        for (JsonNode repo : repositories) {
          String fullName = repo.path("nameWithOwner").asText();
          getRepositoryDetailWithGraphQL(userId, fullName);
        }
      }
    } catch (Exception e) {
      log.error("GraphQL API를 통한 GitHub 레포지토리 정보 저장 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

  /**
   * 비동기적으로 레포지토리 정보를 저장합니다.
   */
  public CompletableFuture<Void> saveAllRepositoriesToMongoAsync(Long userId) {
    return CompletableFuture.runAsync(() -> {
      try {
        log.info("비동기 GitHub 레포지토리 저장 시작: 사용자 ID {}", userId);
        saveAllRepositoriesToMongoWithGraphQL(userId);
        log.info("비동기 GitHub 레포지토리 저장 완료: 사용자 ID {}", userId);
      } catch (Exception e) {
        log.error("비동기 GitHub 레포지토리 저장 중 오류 발생: 사용자 ID {}", userId, e);
        throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
      }
    });
  }

  /**
   * 특정 레포지토리의 전체 디렉토리 구조를 재귀적으로 가져와 MongoDB에 업데이트합니다.
   * @param userId 사용자 ID
   * @param repoFullName 레포지토리 전체 이름 (owner/repo)
   * @param maxDepth 최대 탐색 깊이 (기본값: 3)
   * @return 업데이트된 레포지토리 정보
   */
  public Map<String, Object> updateRepositoryDirectoryStructure(Long userId, String repoFullName, int maxDepth) {
    try {
      GitHubApi gitHubApi = cacheService.findByUserId(userId);

      // 레포지토리 정보 조회
      Map<String, Object> repoDetail = gitHubMongoService.getRepositoryDetailFromMongo(userId, repoFullName);
      if (repoDetail == null) {
        throw new RuntimeException("레포지토리 정보를 찾을 수 없습니다: " + repoFullName);
      }

      // 레포지토리 이름에서 owner와 repo 분리
      String[] parts = repoFullName.split("/");
      if (parts.length != 2) {
        throw new RuntimeException("잘못된 레포지토리 이름 형식입니다: " + repoFullName);
      }
      String owner = parts[0];
      String repo = parts[1];

      // 파일 목록을 재귀적으로 가져오기
      List<Map<String, Object>> files = getCompleteDirectoryStructure(
          gitHubApi.getGithubAccessToken(),
          owner,
          repo,
          "",
          0,
          maxDepth
      );

      // 기존 레포지토리 정보 업데이트
      repoDetail.put("files", files);

      // MongoDB에 저장
      gitHubMongoService.saveRepositoryToMongo(userId, repoDetail);

      log.info("레포지토리 {} 디렉토리 구조 업데이트 완료 (깊이: {})", repoFullName, maxDepth);
      return repoDetail;
    } catch (Exception e) {
      log.error("레포지토리 디렉토리 구조 업데이트 중 오류 발생: {}", repoFullName, e);
      throw new RuntimeException("레포지토리 디렉토리 구조를 업데이트하는데 실패했습니다: " + repoFullName, e);
    }
  }

  /**
   * 특정 디렉토리의 내용을 재귀적으로 가져옵니다.
   * @param token GitHub 액세스 토큰
   * @param owner 레포지토리 소유자
   * @param repo 레포지토리 이름
   * @param path 디렉토리 경로
   * @param currentDepth 현재 탐색 깊이
   * @param maxDepth 최대 탐색 깊이
   * @return 디렉토리 내용 목록
   */
  private List<Map<String, Object>> getCompleteDirectoryStructure(
      String token,
      String owner,
      String repo,
      String path,
      int currentDepth,
      int maxDepth
  ) {
    try {
      // 깊이 제한 초과 시 탐색 중지
      if (currentDepth > maxDepth) {
        return new ArrayList<>();
      }

      // GraphQL 쿼리 생성
      String query = String.format("""
        {
          repository(owner: "%s", name: "%s") {
            object(expression: "HEAD:%s") {
              ... on Tree {
                entries {
                  name
                  type
                  path
                }
              }
            }
          }
        }
    """, owner, repo, path);

      // GraphQL 요청 전송
      JsonNode response = executeGraphQLQuery(token, query);
      JsonNode entriesNode = response.path("data").path("repository").path("object").path("entries");
      List<Map<String, Object>> contents = new ArrayList<>();

      if (!entriesNode.isMissingNode() && entriesNode.isArray()) {
        for (JsonNode entry : entriesNode) {
          Map<String, Object> item = new HashMap<>();
          String entryName = entry.path("name").asText();
          String entryPath = entry.path("path").asText();
          String entryType = entry.path("type").asText();
          boolean isDirectory = "tree".equals(entryType);

          item.put("name", entryName);
          item.put("path", entryPath);
          item.put("type", isDirectory ? "dir" : "file");

          // 디렉토리인 경우 재귀적으로 하위 구조 탐색
          if (isDirectory) {
            // 중요 디렉토리는 깊이에 상관없이 더 깊게 탐색할 수 있음
            int nextMaxDepth = maxDepth;
            if (entryPath.contains("src/") ||
                entryPath.contains("main/java") ||
                entryPath.contains("app/src") ||
                entryName.equals(".github") ||
                entryName.equals("gradle")) {
              nextMaxDepth = maxDepth + 1; // 중요 디렉토리는 한 단계 더 깊게
            }

            List<Map<String, Object>> subDirContents = getCompleteDirectoryStructure(
                token,
                owner,
                repo,
                entryPath,
                currentDepth + 1,
                nextMaxDepth
            );

            if (!subDirContents.isEmpty()) {
              item.put("directory", subDirContents);
            }

            // API 호출 제한을 고려하여 약간의 지연 추가
            try {
              Thread.sleep(100);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
            }
          }

          contents.add(item);
        }
      }

      return contents;
    } catch (Exception e) {
      log.warn("디렉토리 구조 조회 중 오류 발생: {} (깊이: {})", path, currentDepth, e);
      return new ArrayList<>();
    }
  }

  /**
   * 레포지토리 선택 시 전체 디렉토리 구조를 비동기적으로 업데이트합니다.
   * @param userId 사용자 ID
   * @param repoFullName 레포지토리 전체 이름
   * @return CompletableFuture
   */
  public CompletableFuture<Map<String, Object>> updateRepositoryDirectoryStructureAsync(Long userId, String repoFullName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.info("레포지토리 {} 디렉토리 구조 비동기 업데이트 시작", repoFullName);
        Map<String, Object> result = updateRepositoryDirectoryStructure(userId, repoFullName, 3);
        log.info("레포지토리 {} 디렉토리 구조 비동기 업데이트 완료", repoFullName);
        return result;
      } catch (Exception e) {
        log.error("레포지토리 디렉토리 구조 비동기 업데이트 중 오류 발생: {}", repoFullName, e);
        throw new RuntimeException("레포지토리 디렉토리 구조를 업데이트하는데 실패했습니다: " + repoFullName, e);
      }
    });
  }
}