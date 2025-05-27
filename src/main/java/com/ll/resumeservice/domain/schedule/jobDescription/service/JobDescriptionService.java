package com.ll.resumeservice.domain.schedule.jobDescription.service;

import com.ll.resumeservice.domain.schedule.jobDescription.document.JobDescription;
import com.ll.resumeservice.domain.schedule.jobDescription.dto.request.JobDescriptionPatchRequest;
import com.ll.resumeservice.domain.schedule.jobDescription.dto.request.JobDescriptionRequest;
import com.ll.resumeservice.domain.schedule.jobDescription.dto.response.JobDescriptionResponse;
import com.ll.resumeservice.domain.schedule.jobDescription.repository.JobDescriptionRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class JobDescriptionService {

  private final JobDescriptionRepository jobDescriptionRepository;
  private final SecurityFilterChain filterChain;

  @Transactional
  public JobDescriptionResponse createJobDescription(Long spaceId, JobDescriptionRequest request) {
    JobDescription jobDescription = JobDescription.builder()
        .spaceId(spaceId)
        .url(request.getUrl())
        .isManualInput(request.isManualInput())
        .company(request.getCompany())
        .position(request.getPosition())
        .mainTasks(request.getMainTasks())
        .requirements(request.getRequirements())
        .career(request.getCareer())
        .resumeRequirements(request.getResumeRequirements())
        .recruitmentProcess(request.getRecruitmentProcess())
        .publicGrade(request.getPublicGrade())
        .build();

    return JobDescriptionResponse.of(jobDescriptionRepository.save(jobDescription));
  }

  public List<JobDescriptionResponse> getJobDescriptions(Long spaceId) {
    List<JobDescription> jobDescriptions = jobDescriptionRepository.findBySpaceId(spaceId);
    return jobDescriptions.stream()
        .map(JobDescriptionResponse::of)
        .toList();
  }

  public JobDescriptionResponse getJobDescription(Long spaceId, String id) {
    JobDescription jobDescription = jobDescriptionRepository.findByIdAndSpaceId(id,spaceId)
        .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다"));
    return JobDescriptionResponse.of(jobDescription);
  }

  @Transactional
  public void deleteJobDescription(Long spaceId, String id) {
    JobDescription jobDescription = jobDescriptionRepository.findByIdAndSpaceId(id, spaceId)
        .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다"));
    jobDescriptionRepository.delete(jobDescription);
  }

  @Transactional
  public JobDescriptionResponse updateJobDescription(String id, Long spaceId, JobDescriptionPatchRequest request) {
    JobDescription jobDescription = jobDescriptionRepository.findByIdAndSpaceId(id, spaceId)
        .orElseThrow(() -> new RuntimeException("채용공고를 찾을 수 없습니다"));

    Arrays.stream(request.getClass().getDeclaredFields())
        .forEach(field -> {
          try {
            field.setAccessible(true);
            Object value = field.get(request);
            if(value != null){
              String filedName = field.getName();
              String setterName = "set" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1);

              Method setter = JobDescription.class.getMethod(setterName, field.getType());
              setter.invoke(jobDescription, value);
            }
          } catch (Exception e){
            throw new RuntimeException("Failed to update field: " + field.getName(), e);
          }
        });

    return JobDescriptionResponse.of(jobDescriptionRepository.save(jobDescription));
  }
}
