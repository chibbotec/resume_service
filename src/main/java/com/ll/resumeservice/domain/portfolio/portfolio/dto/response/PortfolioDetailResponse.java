package com.ll.resumeservice.domain.portfolio.portfolio.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDetailResponse {

  private String id;

  private Long spaceId;

  private String title;

  private Author author;

  private Duration duration;

  private Contents contents;

  private String thumbnailUrl;

  private boolean publicAccess;

  private List<GitHubRepo> githubRepos;

  private List<SavedFile> savedFiles;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Author {
    private Long id;
    private String nickname;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Duration {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Contents {
    private String techStack;
    private String summary;
    private String description;
    private Map<String, List<String>> features;
    private Architecture architecture;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Architecture {
    private String communication;
    private String deployment;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GitHubRepo {
    private String name;
    private String url;
    private String description;
    private String language;
    private Integer lineCount;
    private Long byteSize;
    private List<String> selectedDirectories;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SavedFile {
    private String id;
    private String name;
    private String path;
    private String repository;
    private String savedPath;
  }

  // Portfolio Document를 PortfolioDetailResponse로 변환하는 메서드
  public static PortfolioDetailResponse of(
      com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio portfolio) {

    if (portfolio == null) {
      return null;
    }

    return PortfolioDetailResponse.builder()
        .id(portfolio.getId())
        .spaceId(portfolio.getSpaceId())
        .title(portfolio.getTitle())
        .author(portfolio.getAuthor() != null ? Author.builder()
            .id(portfolio.getAuthor().getId())
            .nickname(portfolio.getAuthor().getNickname())
            .build() : null)
        .duration(portfolio.getDuration() != null ? Duration.builder()
            .startDate(portfolio.getDuration().getStartDate())
            .endDate(portfolio.getDuration().getEndDate())
            .build() : null)
        .contents(portfolio.getContents() != null ? Contents.builder()
            .techStack(portfolio.getContents().getTechStack())
            .summary(portfolio.getContents().getSummary())
            .description(portfolio.getContents().getDescription())
            .features(portfolio.getContents().getFeatures())
            .architecture(portfolio.getContents().getArchitecture() != null ? Architecture.builder()
                .communication(portfolio.getContents().getArchitecture().getCommunication())
                .deployment(portfolio.getContents().getArchitecture().getDeployment())
                .build() : null)
            .build() : null)
        .thumbnailUrl(portfolio.getThumbnailUrl())
        .publicAccess(portfolio.isPublicAccess())
        .githubRepos(portfolio.getGithubRepos() != null ?
            portfolio.getGithubRepos().stream()
                .map(repo -> GitHubRepo.builder()
                    .name(repo.getName())
                    .url(repo.getUrl())
                    .description(repo.getDescription())
                    .language(repo.getLanguage())
                    .lineCount(repo.getLineCount())
                    .byteSize(repo.getByteSize())
                    .selectedDirectories(repo.getSelectedDirectories())
                    .build())
                .toList() : null)
        .savedFiles(portfolio.getSavedFiles() != null ?
            portfolio.getSavedFiles().stream()
                .map(file -> SavedFile.builder()
                    .id(file.getId())
                    .name(file.getName())
                    .path(file.getPath())
                    .repository(file.getRepository())
                    .savedPath(file.getSavedPath())
                    .build())
                .toList() : null)
        .createdAt(portfolio.getCreatedAt())
        .updatedAt(portfolio.getUpdatedAt())
        .build();
  }
}