package com.ll.resumeservice.domain.resume.controller;


import com.ll.resumeservice.domain.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/resume/{spaceId}/resume")
public class ApiV1ResumeController {

  private final ResumeService resumeService;

}
