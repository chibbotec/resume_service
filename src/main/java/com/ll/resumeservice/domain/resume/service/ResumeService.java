package com.ll.resumeservice.domain.resume.service;


import com.ll.resumeservice.domain.resume.document.Resume;
import com.ll.resumeservice.domain.resume.document.Resume.Author;
import com.ll.resumeservice.domain.resume.dto.request.ResumeCreateRequest;
import com.ll.resumeservice.domain.resume.dto.response.ResumeDetailResponse;
import com.ll.resumeservice.domain.resume.dto.response.ResumeSummaryResponse;
import com.ll.resumeservice.domain.resume.repository.ResumeRepository;
import com.ll.resumeservice.global.client.MemberResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ResumeService {

  private final ResumeRepository resumeRepository;

  public List<ResumeSummaryResponse> getResumeList(Long spaceId, MemberResponse loginUser) {
    List<Resume> resumeList = resumeRepository.findBySpaceIdAndAuthor_Id(spaceId, loginUser.getId());
    return resumeList.stream()
        .map(ResumeSummaryResponse::of)
        .toList();
  }

  public ResumeDetailResponse getResumeDetail(String resumeId, Long userId) {
    Resume resume = resumeRepository.findById(resumeId)
        .orElseThrow(() -> new RuntimeException("이력서를 찾을 수 없습니다"));

    // 작성자 본인만 조회 가능하도록 체크
    if (!resume.getAuthor().getId().equals(userId)) {
      throw new RuntimeException("접근 권한이 없습니다");
    }

    return ResumeDetailResponse.of(resume);
  }

  @Transactional
  public ResumeDetailResponse createResume(Long spaceId, MemberResponse loginUser, ResumeCreateRequest request) {

    Resume resume = Resume.builder()
        .spaceId(spaceId)
        .author(Author.builder()
            .id(loginUser.getId())
            .nickname(loginUser.getNickname())
            .build())
        .title(request.getTitle())
        .name(request.getName())
        .email(request.getEmail())
        .phone(request.getPhone())
        .careerType(request.getCareerType())
        .position(request.getPosition())
        .techStack(request.getTechStack())
        .techSummary(request.getTechSummary())
        .links(request.getLinks().stream().map(
            link -> Resume.Link.builder()
                .type(link.getType())
                .url(link.getUrl())
                .build()
        ).toList())
        .careers(request.getCareers().stream().map(
            career -> Resume.Career.builder()
                .period(career.getPeriod())
                .company(career.getCompany())
                .position(career.getPosition())
                .isCurrent(career.getIsCurrent())
                .startDate(career.getStartDate())
                .endDate(career.getEndDate())
                .description(career.getDescription())
                .achievement(career.getAchievement())
                .build()
        ).toList())
        .projects(request.getProjects().stream().map(
            project -> Resume.Project.builder()
                .name(project.getName())
                .description(project.getDescription())
                .techStack(project.getTechStack())
                .role(project.getRole().stream().toList())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .memberCount(project.getMemberCount())
                .memberRoles(project.getMemberRoles())
                .githubLink(project.getGithubLink())
                .deployLink(project.getDeployLink())
                .build()
        ).toList())
        .educations(request.getEducations().stream().map(
            education -> Resume.Education.builder()
                .school(education.getSchool())
                .major(education.getMajor())
                .degree(education.getDegree())
                .startDate(education.getStartDate())
                .endDate(education.getEndDate())
                .build()
        ).toList())
        .certificates(request.getCertificates().stream().map(
            certificate -> Resume.Certificate.builder()
                .type(certificate.getType())
                .name(certificate.getName())
                .date(certificate.getDate())
                .organization(certificate.getOrganization())
                .build()
        ).toList())
        .coverLetters(request.getCoverLetters().stream().map(
            coverletter -> Resume.CoverLetter.builder()
                .title(coverletter.getTitle())
                .content(coverletter.getContent())
                .build()
            ).toList())
        .build();

    return ResumeDetailResponse.of(resumeRepository.save(resume));
  }
}
