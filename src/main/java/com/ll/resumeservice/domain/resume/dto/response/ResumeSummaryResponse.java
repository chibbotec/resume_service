package com.ll.resumeservice.domain.resume.dto.response;

import com.ll.resumeservice.domain.resume.document.Resume;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeSummaryResponse {
  private String id;
  private String title;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static ResumeSummaryResponse of(Resume resume) {
    if (resume == null) return null;

    return ResumeSummaryResponse.builder()
        .id(resume.getId())
        .title(resume.getTitle())
        .createdAt(resume.getCreatedAt())
        .build();
  }
}