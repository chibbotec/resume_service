package com.ll.resumeservice.domain.schedule.jobApplication.controller;

import com.ll.resumeservice.domain.schedule.jobApplication.dto.request.JobApplicationCreateRequest;
import com.ll.resumeservice.domain.schedule.jobApplication.dto.response.JobApplicationResponse;
import com.ll.resumeservice.domain.schedule.jobApplication.service.JobApplicationService;
import com.ll.resumeservice.global.client.MemberResponse;
import com.ll.resumeservice.global.webMvc.LoginUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume/{spaceId}/job-application")
public class ApiV1JobApplicationController {

  private final JobApplicationService jobApplicationService;
  @PostMapping()
  public ResponseEntity<JobApplicationResponse> crateJobApplication(
      @LoginUser MemberResponse loginUser,
      @PathVariable("spaceId") Long spaceId,
      @ModelAttribute JobApplicationCreateRequest request
  ) {
    return ResponseEntity.ok(jobApplicationService.createJobApplication(spaceId, loginUser, request));
  }

  @GetMapping
  public ResponseEntity<List<JobApplicationResponse>> getJobApplicationList(
      @PathVariable("spaceId") Long spaceId,
      @LoginUser MemberResponse loginUser
  ) {
    return ResponseEntity.ok(jobApplicationService.getJobApplicationList(spaceId, loginUser));
  }

  @GetMapping("/{jobApplicationId}")
  public ResponseEntity<JobApplicationResponse> getJobApplication(
      @PathVariable("jobApplicationId") String jobApplicationId,
      @LoginUser MemberResponse loginUser
  ) {
    return ResponseEntity.ok(jobApplicationService.getJobApplication(jobApplicationId, loginUser.getId()));
  }

  @DeleteMapping("/{jobApplicationId}")
  public ResponseEntity<Void> deleteJobApplication(
      @PathVariable("jobApplicationId") String jobApplicationId,
      @LoginUser MemberResponse loginUser
  ) {
    jobApplicationService.deleteJobApplication(jobApplicationId, loginUser.getId());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{jobApplicationId}/process")
  public ResponseEntity<JobApplicationResponse> updateJobApplicationProcess(
      @PathVariable("jobApplicationId") String jobApplicationId,
      @LoginUser MemberResponse loginUser,
      @RequestBody String processStatus
  ) {
    return ResponseEntity.ok(jobApplicationService.updateJobApplicationProcess(jobApplicationId, loginUser.getId(), processStatus));
  }
}
