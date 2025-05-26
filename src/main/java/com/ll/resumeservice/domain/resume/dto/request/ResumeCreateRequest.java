package com.ll.resumeservice.domain.resume.dto.request;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeCreateRequest {
  private String title;          // 이력서 제목
  private String name;           // 이름
  private String email;          // 이메일
  private String phone;          // 전화번호
  private String careerType;     // "신입" 또는 "경력"
  private String position;       // 지원 포지션
  private List<String> techStack;    // 기술 스택 배열
  private String techSummary;    // 기술 스택 요약
  private List<LinkDto> links;   // 링크 정보
  private List<CareerDto> careers;   // 경력 정보
  private List<ProjectDto> projects; // 프로젝트 정보
  private List<EducationDto> educations; // 교육 정보
  private List<CertificateDto> certificates; // 자격증/수상 정보
  private List<CoverLetter> coverLetters; // 커버레터 정보

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LinkDto {
    private String type;  // "github", "notion", "blog"
    private String url;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CareerDto {
    private String period;         // 경력 기간
    private String company;        // 회사명
    private String position;       // 직급
    private Boolean isCurrent;     // 재직중 여부
    private LocalDate startDate;   // 입사일
    private LocalDate endDate;     // 퇴사일
    private String description;    // 직무 내용
    private String achievement;    // 주요 성과
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectDto {
    private String name;           // 프로젝트명
    private String description;    // 프로젝트 설명
    private List<String> techStack;    // 사용 기술 스택
    private String role;           // 담당 역할
    private LocalDate startDate;   // 시작일
    private LocalDate endDate;     // 종료일
    private Integer memberCount;   // 팀원 수
    private String memberRoles;     // 팀 내 역할
    private String githubLink;     // 깃허브 링크
    private String deployLink;     // 배포 링크
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EducationDto {
    private String school;         // 학교/기관명
    private String major;          // 전공
    private LocalDate startDate;   // 입학일
    private LocalDate endDate;     // 졸업일
    private String degree;         // 학위 (졸업예정, 졸업, 중퇴, 수료예정, 수료)
    private String note;           // 비고
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CertificateDto {
    private String type;           // "자격증" 또는 "수상경력"
    private String name;           // 자격증명/수상내역
    private LocalDate date;        // 취득/수상일
    private String organization;   // 주관기관
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverLetter {
    private String title;
    private String content;
  }
}
