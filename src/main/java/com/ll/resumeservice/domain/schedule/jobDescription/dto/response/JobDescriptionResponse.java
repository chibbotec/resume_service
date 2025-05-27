package com.ll.resumeservice.domain.schedule.jobDescription.dto.response;

import com.ll.resumeservice.domain.schedule.jobDescription.document.JobDescription;
import com.ll.resumeservice.domain.schedule.jobDescription.grade.PublicGrade;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobDescriptionResponse {

  private String id;
  private String url;                    // 채용공고 URL (직접 입력인 경우 null)
  private boolean isManualInput;         // 직접 입력 여부
  private String company;                // 채용 기업
  private String position;               // 포지션 상세
  private List<String> mainTasks;        // 주요업무
  private List<String> requirements;     // 자격요건
  private String career;                 // 경력
  private List<String> resumeRequirements; // 이력서 포함 사항
  private List<String> recruitmentProcess; // 채용 절차
  private Long spaceId;                 // 스페이스 ID
  private LocalDateTime createdAt;       // 생성 시간
  private LocalDateTime updatedAt;       // 수정 시간
  private PublicGrade publicGrade;

  public static JobDescriptionResponse of(JobDescription jobDescription) {
    return JobDescriptionResponse.builder()
        .id(jobDescription.getId())
        .url(jobDescription.getUrl())
        .isManualInput(jobDescription.isManualInput())
        .company(jobDescription.getCompany())
        .position(jobDescription.getPosition())
        .mainTasks(jobDescription.getMainTasks())
        .requirements(jobDescription.getRequirements())
        .career(jobDescription.getCareer())
        .resumeRequirements(jobDescription.getResumeRequirements())
        .recruitmentProcess(jobDescription.getRecruitmentProcess())
        .spaceId(jobDescription.getSpaceId())
        .createdAt(jobDescription.getCreatedAt())
        .updatedAt(jobDescription.getUpdatedAt())
        .publicGrade(jobDescription.getPublicGrade())
        .build();
  }
}
