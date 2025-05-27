package com.ll.resumeservice.domain.schedule.jobDescription.document;

import com.ll.resumeservice.domain.schedule.jobDescription.grade.PublicGrade;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "job_descriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {
  @Id
  private String id;                    // MongoDB ObjectId

  private String url;                    // 채용공고 URL (직접 입력인 경우 null)
  private boolean isManualInput;         // 직접 입력 여부
  private String company;                // 채용 기업
  private String position;               // 포지션 상세
  private List<String> mainTasks;        // 주요업무
  private List<String> requirements;     // 자격요건
  private String career;                 // 경력
  private List<String> resumeRequirements; // 이력서 포함 사항
  private List<String> recruitmentProcess; // 채용 절차

  @Enumerated(EnumType.STRING)
  @Column(name = "public_grade")
  private PublicGrade publicGrade;

  private Long spaceId;                 // 스페이스 ID

  @CreatedDate
  private LocalDateTime createdAt;       // 생성 시간

  @LastModifiedDate
  private LocalDateTime updatedAt;       // 수정 시간
}