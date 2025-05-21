package com.ll.resumeservice.domain.portfolio.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.resumeservice.domain.portfolio.github.eventListener.GitHubLoginEvent;
import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubApiRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class GitHubApiService {

  private final GitHubApiRepository gitHubApiRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RestTemplate restTemplate = new RestTemplate();
  private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

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

  //  @Cacheable(value = "githubConnections", key = "#userId")
  public GitHub getGitHubConnection(Long userId) throws IOException {
    // GitHub 연결 로직
    // 이 결과는 캐시에 저장되고 다음 호출 시 재사용됩니다
    log.info("GitHub 연결을 생성합니다: {}", userId);
    GitHubApi gitHubApi = findByUserId(userId);
    return new GitHubBuilder()
        .withOAuthToken(gitHubApi.getGithubAccessToken())
        .build();
  }

  //  @Cacheable(value = "githubApis", key = "#userId")
  public GitHubApi findByUserId(Long userId) {
    // DB에서 사용자 정보 조회 로직
    log.info("DB에서 GitHub API 정보를 조회합니다: {}", userId);
    return gitHubApiRepository.findByUserId(userId);
  }

  public JsonNode executeGraphQLQuery(GitHubApi gitHubApi, String query, Map<String, Object> variables) throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(gitHubApi.getGithubAccessToken());

    Map<String, Object> requestBody = Map.of(
        "query", query,
        "variables", variables
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

  public Date parseDate(String dateString) {
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