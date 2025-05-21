package com.ll.resumeservice.domain.portfolio.github.document;

import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repository_commit_details")
@CompoundIndex(def = "{'userId': 1, 'repository': 1}", unique = true)
public class GithubCommit {
  @Id
  private String id;

  private Long userId;
  private String repository;
  private Date createdAt;
  private Date updatedAt;

  private List<CommitDocument> commits;

  private int totalCommits;
  private int totalBranches;
  private int totalAdditions;
  private int totalDeletions;
  private Set<String> contributors;
  private Set<String> branches;

  @Data
  @Builder
  public static class CommitDocument {
    private String branch;
    private String sha;
    private String nodeId;
    private String message;
    private String author;
    private String email;
    private Date date;
    private String url;
    private int totalChanges;
    private int additions;
    private int deletions;
    private List<FileDocument> files;
  }

  @Data
  @Builder
  public static class FileDocument {
    private String sha;
    private String filename;
    private String status;
    private int additions;
    private int deletions;
    private int changes;
    private String patch;
    private String blobUrl;
  }
}