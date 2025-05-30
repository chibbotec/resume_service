package com.ll.resumeservice.domain.schedule.jobApplication.service;

import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.Portfolio;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.Resume;
import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication.SavedFile;
import com.ll.resumeservice.domain.schedule.jobApplication.dto.request.JobApplicationCreateRequest;
import com.ll.resumeservice.domain.schedule.jobApplication.dto.response.JobApplicationResponse;
import com.ll.resumeservice.domain.schedule.jobApplication.process.ProcessStatus;
import com.ll.resumeservice.domain.schedule.jobApplication.repository.JobApplicationRepository;
import com.ll.resumeservice.global.client.MemberResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class JobApplicationService {

  @Value("${files.applicationJob.path}")
  private String storageBasePath;

  private final JobApplicationRepository jobApplicationRepository;

  @Transactional
  public JobApplicationResponse createJobApplication(Long spaceId, MemberResponse loginUser, JobApplicationCreateRequest request) {
    // 1. 모든 파일 저장 및 SavedFile 리스트 생성
    List<JobApplication.SavedFile> savedFiles = new ArrayList<>();

    if (request.getFiles() != null && !request.getFiles().isEmpty()) {
      for (MultipartFile file : request.getFiles()) {
        if (file != null && !file.isEmpty()) {
          String originalFileName = file.getOriginalFilename();
          String uuidFileName = generateUuidFileName(originalFileName);

          try {
            // 실제 파일 저장 로직 (파일 서비스 호출)
            saveFile(file, uuidFileName);

            // SavedFile 객체 생성하여 리스트에 추가
            JobApplication.SavedFile savedFile = JobApplication.SavedFile.builder()
                .oringinalFileName(originalFileName)
                .uuidFileName(uuidFileName)
                .build();
            savedFiles.add(savedFile);

            log.info("File saved successfully: {} -> {}", originalFileName, uuidFileName);
          } catch (Exception e) {
            log.error("Failed to save file: {}", originalFileName, e);
            // 이미 저장된 파일들 정리
            cleanupSavedFiles(savedFiles);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다: " + originalFileName, e);
          }
        }
      }
    }

    JobApplication jobApplication = JobApplication.builder()
        .spaceId(spaceId)
        .userId(loginUser.getId())
        .company(request.getCompany())
        .position(request.getPosition())
        .processStatus(ProcessStatus.RESUME)
        .platform(request.getPlatform())
        .resume(Resume.builder()
            .id((request.getResume() != null) ? request.getResume().getId() : null)
            .title(request.getResume() != null ? request.getResume().getTitle() : null)
            .build())
        .portfolios(request.getParsedPortfolios().stream()  // 여기가 변경됨
            .map(
                portfolio -> Portfolio.builder()
                    .id(portfolio.getId())
                    .title(portfolio.getTitle())
                    .build()
            ).toList())
        .files(savedFiles)
        .build();

    return JobApplicationResponse.from(jobApplicationRepository.save(jobApplication));
  }

  // 실제 파일 저장 메서드 (파일 시스템에 직접 저장)
  private void saveFile(MultipartFile file, String uuidFileName) throws Exception {
    // 저장 경로 생성
    Path storagePath = Paths.get(storageBasePath);

    // 디렉토리가 없으면 생성
    if (!Files.exists(storagePath)) {
      Files.createDirectories(storagePath);
    }

    // 파일 저장
    Path filePath = storagePath.resolve(uuidFileName);
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
  }

  // 저장된 파일들 정리 메서드 (오류 발생 시)
  private void cleanupSavedFiles(List<SavedFile> savedFiles) {
    for (JobApplication.SavedFile savedFile : savedFiles) {
      try {
        Path filePath = Paths.get(storageBasePath).resolve(savedFile.getUuidFileName());
        if (Files.exists(filePath)) {
          Files.delete(filePath);
          log.info("Cleaned up file: {}", savedFile.getUuidFileName());
        }
      } catch (Exception e) {
        log.warn("Failed to cleanup file: {}", savedFile.getUuidFileName(), e);
      }
    }
  }

  // 파일명 생성 헬퍼 메서드
  private String generateUuidFileName(String originalFileName) {
    if (originalFileName == null) return null;

    String extension = "";
    int lastDotIndex = originalFileName.lastIndexOf(".");
    if (lastDotIndex > 0) {
      extension = originalFileName.substring(lastDotIndex);
    }

    return UUID.randomUUID().toString() + extension;
  }

  public List<JobApplicationResponse> getJobApplicationList(Long spaceId, MemberResponse loginUser) {
    List<JobApplication> jobApplications = jobApplicationRepository.findBySpaceIdAndUserId(spaceId, loginUser.getId());

    if (jobApplications.isEmpty()) {
      return List.of();
    }

    return jobApplications.stream()
        .map(JobApplicationResponse::from)
        .toList();
  }

  public JobApplicationResponse getJobApplication(String jobApplicationId, Long id) {
    JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
        .orElseThrow(() -> new RuntimeException("Job Application not found with id: " + jobApplicationId));

    if (!jobApplication.getUserId().equals(id)) {
      throw new RuntimeException("Unauthorized access to this job application");
    }

    return JobApplicationResponse.from(jobApplication);
  }

  @Transactional
  public void deleteJobApplication(String jobApplicationId, Long id) {
    JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
        .orElseThrow(() -> new RuntimeException("Job Application not found with id: " + jobApplicationId));

    if (!jobApplication.getUserId().equals(id)) {
      throw new RuntimeException("Unauthorized access to this job application");
    }

    // 파일 삭제 로직
    for (SavedFile file : jobApplication.getFiles()) {
      try {
        Path filePath = Paths.get(storageBasePath).resolve(file.getUuidFileName());
        if (Files.exists(filePath)) {
          Files.delete(filePath);
          log.info("Deleted file: {}", file.getUuidFileName());
        }
      } catch (Exception e) {
        log.warn("Failed to delete file: {}", file.getUuidFileName(), e);
      }
    }

    jobApplicationRepository.delete(jobApplication);
  }

  @Transactional
  public JobApplicationResponse updateJobApplicationProcess(String jobApplicationId, Long id, String processStatus) {
    JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
        .orElseThrow(() -> new RuntimeException("Job Application not found with id: " + jobApplicationId));

    if (!jobApplication.getUserId().equals(id)) {
      throw new RuntimeException("Unauthorized access to this job application");
    }

    ProcessStatus status;
    try {
      status = ProcessStatus.valueOf(processStatus.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid process status: " + processStatus);
    }

    jobApplication.setProcessStatus(status);
    return JobApplicationResponse.from(jobApplicationRepository.save(jobApplication));
  }
}
