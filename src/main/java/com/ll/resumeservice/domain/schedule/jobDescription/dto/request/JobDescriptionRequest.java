package com.ll.resumeservice.domain.schedule.jobDescription.dto.request;

import com.ll.resumeservice.domain.schedule.jobDescription.grade.PublicGrade;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescriptionRequest {
  private String url;                    // 채용공고 URL (직접 입력인 경우 null)
  private boolean isManualInput;         // 직접 입력 여부
  private String company;                // 채용 기업
  private String position;               // 포지션 상세
  private List<String> mainTasks;        // 주요업무
  private List<String> requirements;     // 자격요건
  private String career;                 // 경력
  private List<String> resumeRequirements; // 이력서 포함 사항
  private List<String> recruitmentProcess; // 채용 절차
  private PublicGrade publicGrade;
}