package com.ll.resumeservice.global.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceClient {

  private final RestTemplate restTemplate;

  @Value("${services.member.url}")
  private String memberServiceUrl;

  public MemberResponse getMemberByUsername(String username) {
    try {
      // 멤버 서비스 URL 로깅
      log.info("Member Service URL: {}", memberServiceUrl);

      // 전체 URL 생성 및 로깅
      String url = UriComponentsBuilder.fromUriString(memberServiceUrl)
          .path("/api/v1/members/{username}")
          .buildAndExpand(username)
          .toUriString();

      log.info("Full URL for member request: {}", url);

      // 요청 시도 로깅
      log.info("Attempting to fetch member with id: {}", username);

      // RestTemplate 요청 및 응답 로깅
      MemberResponse member = restTemplate.getForObject(url, MemberResponse.class);

      log.info("Successfully fetched member: {}", member);

      return member;
    } catch (Exception e) {
      // 예외 상세 로깅
      log.error("Error fetching member with id {}: {}", username, e.getMessage(), e);

      MemberResponse member = new MemberResponse();
      member.setUsername(username);

      log.info("Returning default MemberDto with id: {}", username);

      return member;
    }
  }
}