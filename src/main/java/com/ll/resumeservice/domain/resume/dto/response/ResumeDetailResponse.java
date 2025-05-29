package com.ll.resumeservice.domain.resume.dto.response;

import com.ll.resumeservice.domain.resume.document.Resume;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailResponse {
  private String id;
  private Long spaceId;
  private AuthorDto author;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String title;
  private String name;
  private String email;
  private String phone;
  private String careerType;
  private String position;
  private List<String> techStack;
  private String techSummary;
  private List<LinkDto> links;
  private List<CareerDto> careers;
  private List<ProjectDto> projects;
  private List<EducationDto> educations;
  private List<CertificateDto> certificates;
  private List<CoverletterDto> coverletters;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AuthorDto {
    private Long id;
    private String nickname;

    public static AuthorDto of(Resume.Author author) {
      if (author == null) return null;
      return AuthorDto.builder()
          .id(author.getId())
          .nickname(author.getNickname())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LinkDto {
    private String type;
    private String url;

    public static LinkDto of(Resume.Link link) {
      if (link == null) return null;
      return LinkDto.builder()
          .type(link.getType())
          .url(link.getUrl())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CareerDto {
    private String period;
    private String company;
    private String position;
    private Boolean isCurrent;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String achievement;

    public static CareerDto of(Resume.Career career) {
      if (career == null) return null;
      return CareerDto.builder()
          .period(career.getPeriod())
          .company(career.getCompany())
          .position(career.getPosition())
          .isCurrent(career.getIsCurrent())
          .startDate(career.getStartDate())
          .endDate(career.getEndDate())
          .description(career.getDescription())
          .achievement(career.getAchievement())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectDto {
    private String name;
    private String description;
    private List<String> techStack;
    private List<String> role;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer memberCount;
    private String memberRoles;
    private String githubLink;
    private String deployLink;

    public static ProjectDto of(Resume.Project project) {
      if (project == null) return null;
      return ProjectDto.builder()
          .name(project.getName())
          .description(project.getDescription())
          .techStack(project.getTechStack())
          .role(project.getRole())
          .startDate(project.getStartDate())
          .endDate(project.getEndDate())
          .memberCount(project.getMemberCount())
          .memberRoles(project.getMemberRoles())
          .githubLink(project.getGithubLink())
          .deployLink(project.getDeployLink())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EducationDto {
    private String school;
    private String major;
    private LocalDate startDate;
    private LocalDate endDate;
    private String degree;
    private String note;

    public static EducationDto of(Resume.Education education) {
      if (education == null) return null;
      return EducationDto.builder()
          .school(education.getSchool())
          .major(education.getMajor())
          .startDate(education.getStartDate())
          .endDate(education.getEndDate())
          .degree(education.getDegree())
          .note(education.getNote())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CertificateDto {
    private String type;
    private String name;
    private LocalDate date;
    private String organization;

    public static CertificateDto of(Resume.Certificate certificate) {
      if (certificate == null) return null;
      return CertificateDto.builder()
          .type(certificate.getType())
          .name(certificate.getName())
          .date(certificate.getDate())
          .organization(certificate.getOrganization())
          .build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverletterDto {
    private String title;
    private String content;

    public static CoverletterDto of(Resume.CoverLetter coverLetter) {
      if (coverLetter == null) return null;
      return CoverletterDto.builder()
          .title(coverLetter.getTitle())
          .content(coverLetter.getContent())
          .build();
    }
  }

  public static ResumeDetailResponse of(Resume resume) {
    if (resume == null) return null;

    return ResumeDetailResponse.builder()
        .id(resume.getId())
        .spaceId(resume.getSpaceId())
        .author(AuthorDto.of(resume.getAuthor()))
        .createdAt(resume.getCreatedAt())
        .updatedAt(resume.getUpdatedAt())
        .title(resume.getTitle())
        .name(resume.getName())
        .email(resume.getEmail())
        .phone(resume.getPhone())
        .careerType(resume.getCareerType())
        .position(resume.getPosition())
        .techStack(resume.getTechStack())
        .techSummary(resume.getTechSummary())
        .links(resume.getLinks() != null ?
            resume.getLinks().stream()
                .map(LinkDto::of)
                .collect(Collectors.toList()) : null)
        .careers(resume.getCareers() != null ?
            resume.getCareers().stream()
                .map(CareerDto::of)
                .collect(Collectors.toList()) : null)
        .projects(resume.getProjects() != null ?
            resume.getProjects().stream()
                .map(ProjectDto::of)
                .collect(Collectors.toList()) : null)
        .educations(resume.getEducations() != null ?
            resume.getEducations().stream()
                .map(EducationDto::of)
                .collect(Collectors.toList()) : null)
        .certificates(resume.getCertificates() != null ?
            resume.getCertificates().stream()
                .map(CertificateDto::of)
                .collect(Collectors.toList()) : null)
        .coverletters(resume.getCoverLetters() != null ?
            resume.getCoverLetters().stream()
                .map(CoverletterDto::of)
                .collect(Collectors.toList()) : null)
        .build();
  }
}