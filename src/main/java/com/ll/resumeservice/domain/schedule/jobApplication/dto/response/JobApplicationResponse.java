package com.ll.resumeservice.domain.schedule.jobApplication.dto.response;

import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.Portfolio;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.Resume;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.SavedFile;
import com.ll.resumeservice.domain.schedule.jobApplication.process.ProcessStatus;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobApplicationResponse {
  private String id;
  private Long userId;          // 사용자 ID (유저 식별용)
  private Long spaceId;         // 스페이스 ID (공간 식별용)
  private String company;          // 회사명
  private String position;         // 직무
  private String platform;         // 플랫폼 (잡코리아, 사람인, 원티드, 기타)
  private ProcessStatus processStatus;
  private Resume resume;         // 선택된 이력서 ID
  private List<Portfolio> portfolios;     // 선택된 포트폴리오 ID 리스트 (JSON 문자열로 저장)
  private List<SavedFile> files; // 실제 파일 데이터 (JSON 문자열로 저장)

  public static JobApplicationResponse from(JobApplication jobApplication) {
    return JobApplicationResponse.builder()
        .id(jobApplication.getId())
        .userId(jobApplication.getUserId())
        .spaceId(jobApplication.getSpaceId())
        .company(jobApplication.getCompany())
        .position(jobApplication.getPosition())
        .platform(jobApplication.getPlatform())
        .processStatus(jobApplication.getProcessStatus())
        .resume(jobApplication.getResume() != null ? jobApplication.getResume() : null)
        .portfolios(jobApplication.getPortfolios())
        .files(jobApplication.getFiles())
        .build();
  }
}
