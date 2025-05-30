package com.ll.resumeservice.domain.portfolio.github.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TaskResponse {
  private String taskId;
  private String message;
}