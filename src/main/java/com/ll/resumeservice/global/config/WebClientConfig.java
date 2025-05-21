package com.ll.resumeservice.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient webClient() {
    // 타임아웃 설정을 위한 HttpClient
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(30));

    return WebClient.builder()
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  // GitHub API 전용 WebClient (필요시)
  @Bean
  public WebClient githubWebClient() {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(60)); // GitHub API는 좀 더 긴 타임아웃

    return WebClient.builder()
        .baseUrl("https://api.github.com")
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(20 * 1024 * 1024)) // 20MB (큰 커밋 데이터 처리)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
        .defaultHeader(HttpHeaders.USER_AGENT, "Spring-WebClient")
        .build();
  }
}