package com.ll.resumeservice.domain.schedule.jobDescription.dto.request;

import java.util.List;
import lombok.Data;

@Data
public class JobDescriptionPatchRequest {
  private String company;
  private String position;
  private List<String> mainTasks;
  private List<String> requirements;
  private String career;
  private List<String> resumeRequirements;
  private List<String> recruitmentProcess;
  private String publicGrade;
}
