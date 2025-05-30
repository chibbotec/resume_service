package com.ll.resumeservice.domain.schedule.jobApplication.dto.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter  // Setter 추가
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class JobApplicationCreateRequest {
  private String company;
  private String position;
  private String platform;
  private ResumeDto resume;
  private String portfolios; // JSON 문자열로 받기
  private List<MultipartFile> files;

  // portfolios JSON 문자열을 List<PortfolioDto>로 변환하는 메서드
  public List<PortfolioDto> getParsedPortfolios() {
    if (portfolios == null || portfolios.trim().isEmpty()) {
      return new ArrayList<>();
    }

    try {
      ObjectMapper objectMapper = new ObjectMapper();
      return objectMapper.readValue(portfolios, new TypeReference<List<PortfolioDto>>() {});
    } catch (JsonProcessingException e) {
      log.error("Failed to parse portfolios JSON: {}", portfolios, e);
      return new ArrayList<>();
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ResumeDto {
    private String id;
    private String title;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PortfolioDto {
    private String id;
    private String title;
  }
}