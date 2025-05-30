package com.ll.resumeservice.domain.schedule.jobApplication.document;

import com.ll.resumeservice.domain.schedule.jobApplication.process.ProcessStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "job_application")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {
  @Id
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

  @CreatedDate
  private LocalDateTime createdAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Resume {
    @Field("id")
    private String id;
    @Field("title")
    private String title;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Portfolio {
    @Field("id")
    private String id;
    @Field("title")
    private String title;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SavedFile {
    @Field("Oringinal_fileName")
    private String oringinalFileName;
    @Field("uuid_fileName")
    private String uuidFileName;
  }
}
