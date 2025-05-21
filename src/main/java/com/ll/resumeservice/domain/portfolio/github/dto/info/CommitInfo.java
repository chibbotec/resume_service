package com.ll.resumeservice.domain.portfolio.github.dto.info;

import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommitInfo {
  private String branch;
  private String oid;
  private String message;
  private Date committedDate;
  private String url;
  private String authorName;
  private String authorEmail;
  private int additions;
  private int deletions;
}