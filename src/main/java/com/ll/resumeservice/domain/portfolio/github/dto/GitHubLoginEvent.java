package com.ll.resumeservice.domain.portfolio.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubLoginEvent {
  private Long userId;
  private String username;
  private String email;
  private String nickname;

  // GitHub 관련 정보
  private String githubUsername;
  private String githubAccessToken;
  private LocalDateTime githubTokenExpires;
  private String githubScopes;
  private String providerId;
  private String providerType;
}