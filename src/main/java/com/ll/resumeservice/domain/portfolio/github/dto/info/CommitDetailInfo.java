package com.ll.resumeservice.domain.portfolio.github.dto.info;

import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommitDetailInfo {
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
  private List<FileChangeInfo> files;
}
