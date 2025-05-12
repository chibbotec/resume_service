package com.ll.resumeservice.domain.portfolio.portfolio.document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "portfolios")
public class Portfolio {

  @Id
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

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
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
    private Map<String, List<String>> features; // 기능들을 Map으로 저장
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
}