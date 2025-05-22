package com.ll.resumeservice.domain.portfolio.github.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class RepoCommitRequest {

  @NotEmpty(message = "레포지토리 목록은 비어있을 수 없습니다")
  @Size(max = 10, message = "한 번에 최대 10개의 레포지토리만 조회할 수 있습니다")
  private List<@Pattern(regexp = "^[a-zA-Z0-9._-]+/[a-zA-Z0-9._-]+$",
      message = "레포지토리 형식이 올바르지 않습니다 (owner/repo)") String> repoNames;
}
