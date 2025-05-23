package com.ll.resumeservice.domain.resume.controller;


import com.ll.resumeservice.domain.resume.dto.request.ResumeCreateRequest;
import com.ll.resumeservice.domain.resume.dto.response.ResumeDetailResponse;
import com.ll.resumeservice.domain.resume.dto.response.ResumeSummaryResponse;
import com.ll.resumeservice.domain.resume.service.ResumeService;
import com.ll.resumeservice.global.client.MemberResponse;
import com.ll.resumeservice.global.webMvc.LoginUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume/{spaceId}/resume")
public class ApiV1ResumeController {

  private final ResumeService resumeService;

  @GetMapping()
  public ResponseEntity<List<ResumeSummaryResponse>> getResumeList(
      @PathVariable("spaceId") Long spaceId,
      @LoginUser MemberResponse loginUser
  ) {
    return ResponseEntity.ok(resumeService.getResumeList(spaceId, loginUser));
  }

  @GetMapping("/{resumeId}")
  public ResponseEntity<ResumeDetailResponse> getResume(
      @PathVariable("resumeId") String resumeId,
      @LoginUser MemberResponse loginUser
  ) {
    return ResponseEntity.ok(resumeService.getResumeDetail(resumeId, loginUser.getId()));
  }

  @PostMapping()
  public ResponseEntity<ResumeDetailResponse> createResume(
      @LoginUser MemberResponse loginUser,
      @PathVariable("spaceId") Long spaceId,
      @RequestBody ResumeCreateRequest request
  ) {

    return ResponseEntity.ok(resumeService.createResume(spaceId, loginUser, request));
  }

}
