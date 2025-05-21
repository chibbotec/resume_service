package com.ll.resumeservice.domain.resume.service;


import com.ll.resumeservice.domain.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeService {

  private final ResumeRepository resumeRepository;

}
