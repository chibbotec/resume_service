package com.ll.resumeservice.domain.portfolio.portfolio.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class PortfolioPutRequest {

  private String title;

  private AuthorRequest author;

  private DurationRequest duration;

  private String githubLink;

  private String deployLink;

  private Integer memberCount;

  private String memberRoles;

  private ContentsRequest contents;

//  private String thumbnailUrl;

  private boolean publicAccess;

  private List<GitHubRepoRequest> githubRepos;

  private List<SavedFileRequest> savedFiles;

  @Getter
  @Setter
  public static class AuthorRequest {
    private Long id;
    private String nickname;
  }

  @Getter
  @Setter
  public static class DurationRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
  }

  @Getter
  @Setter
  public static class ContentsRequest {
    private String techStack;
    private String summary;
    private String description;
    private List<String> roles;
    private Map<String, List<String>> features;
    private ArchitectureRequest architecture;
  }

  @Getter
  @Setter
  public static class ArchitectureRequest {
    private String communication;
    private String deployment;
  }

  @Getter
  @Setter
  public static class GitHubRepoRequest {
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
  public static class SavedFileRequest {
    private String id;
    private String name;
    private String path;
    private String repository;
    private String savedPath;
  }
}