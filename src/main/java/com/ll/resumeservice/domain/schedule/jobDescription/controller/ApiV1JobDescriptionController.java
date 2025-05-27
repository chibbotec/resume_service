package com.ll.resumeservice.domain.schedule.jobDescription.controller;


import com.ll.resumeservice.domain.schedule.jobDescription.dto.request.JobDescriptionPatchRequest;
import com.ll.resumeservice.domain.schedule.jobDescription.dto.request.JobDescriptionRequest;
import com.ll.resumeservice.domain.schedule.jobDescription.dto.response.JobDescriptionResponse;
import com.ll.resumeservice.domain.schedule.jobDescription.service.JobDescriptionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resume/{spaceId}/job-description")
@RequiredArgsConstructor
@Slf4j
public class ApiV1JobDescriptionController {

  private final JobDescriptionService jobDescriptionService;

  @GetMapping
  public ResponseEntity<List<JobDescriptionResponse>> getJobDescriptions(
      @PathVariable("spaceId") Long spaceId) {

    List<JobDescriptionResponse> jobDescriptions = jobDescriptionService.getJobDescriptions(spaceId);

    return ResponseEntity.ok(jobDescriptions);
  }

  @GetMapping("/{id}")
  public ResponseEntity<JobDescriptionResponse> getJobDescription(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("id") String id
  ) {
    return ResponseEntity.ok(jobDescriptionService.getJobDescription(spaceId, id));
  }

  @PostMapping
  public ResponseEntity<JobDescriptionResponse> createJobDescription(
      @PathVariable("spaceId") Long spaceId,
      @RequestBody JobDescriptionRequest request
  ) {
    return ResponseEntity.ok(jobDescriptionService.createJobDescription(spaceId, request));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<JobDescriptionResponse> updateJobDescription(
      @PathVariable("id") String id,
      @PathVariable("spaceId") Long spaceId,
      @RequestBody JobDescriptionPatchRequest request
  ){
    return ResponseEntity.ok(jobDescriptionService.updateJobDescription(id, spaceId, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteJobDescription(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("id") String id
  ) {
    jobDescriptionService.deleteJobDescription(spaceId, id);
    return ResponseEntity.noContent().build();
  }
}
