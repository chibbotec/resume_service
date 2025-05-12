package com.ll.resumeservice.domain.portfolio.github.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repository_files")
public class GitHubRepository {
  @Id
  private String id;
  private Long userId;
  private Long repoId;
  private String name;
  private String fullName;
  private String description;
  private String url;
  private String apiUrl;
  private String gitUrl;
  private String sshUrl;
  private String homepage;
  private String language;
  private String defaultBranch;
  private Boolean isPrivate;
  private Boolean isFork;
  private Boolean isArchived;
  private Boolean isDisabled;
  private Integer stars;
  private Integer watchers;
  private Integer forks;
  private Integer size;
  private Date createdAt;
  private Date updatedAt;
  private Date pushedAt;
  private List<Map<String, Object>> files;
  private Date savedAt;
  private  String commitSha;

  public void updateFrom(
      String name,
      String fullName,
      String description,
      String url,
      String sshUrl,
      String homepage,
      String language,
      String defaultBranch,
      boolean isPrivate,
      boolean isFork,
      boolean isArchived,
      boolean isDisabled,
      int stars,
      int watchers,
      int forks,
      int size,
      Date createdAt,
      Date updatedAt,
      Date pushedAt,
      String commitSha,
      List<Map<String, Object>> files,
      Date savedAt
  ) {
    this.name = name;
    this.fullName = fullName;
    this.description = description;
    this.url = url;
    this.sshUrl = sshUrl;
    this.homepage = homepage;
    this.language = language;
    this.defaultBranch = defaultBranch;
    this.isPrivate = isPrivate;
    this.isFork = isFork;
    this.isArchived = isArchived;
    this.isDisabled = isDisabled;
    this.stars = stars;
    this.watchers = watchers;
    this.forks = forks;
    this.size = size;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.pushedAt = pushedAt;
    this.commitSha = commitSha;
    this.files = files;
    this.savedAt = savedAt;
  }
}