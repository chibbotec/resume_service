package com.ll.resumeservice.domain.portfolio.github.document;

import java.util.Date;
import java.util.List;
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
@Document(collection = "commit_history")
@CompoundIndex(def = "{'userId': 1, 'repository': 1}", unique = true)
public class CommitHistory {
  @Id
  private String id;

  private Long userId;
  private String repository;

  private List<GroupByFiles> commits;

  @Data
  @Builder
  public static class GroupByFiles {
    private String filename;
    private List<FileHistory> fileHistories;
  }

  @Data
  @Builder
  public static class FileHistory {
    private String branch;
    private Date commitDate;
    private String patch;
  }
}
