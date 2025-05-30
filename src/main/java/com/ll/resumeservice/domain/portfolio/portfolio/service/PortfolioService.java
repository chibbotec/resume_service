package com.ll.resumeservice.domain.portfolio.portfolio.service;

import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Architecture;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Author;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Contents;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Duration;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.GitHubRepo;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.SavedFile;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.request.PortfolioPutRequest;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.response.CatergorizedPortfolioResponse;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.request.PortfolioCreateRequest;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.response.PortfolioDetailResponse;
import com.ll.resumeservice.domain.portfolio.portfolio.repository.PortfolioRepository;
import com.ll.resumeservice.global.error.ErrorCode;
import com.ll.resumeservice.global.exception.CustomException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

  private final PortfolioRepository portfolioRepository;

  public CatergorizedPortfolioResponse getPortfolioList(Long spaceId, Long memberId) {

    List<Portfolio> publicPortfolio = portfolioRepository.findAllBySpaceIdAndPublicAccess(spaceId, true);

    List<Portfolio> privatePortfolio = portfolioRepository.findAllBySpaceIdAndAuthor_Id(spaceId, memberId);
    return CatergorizedPortfolioResponse.of(publicPortfolio, privatePortfolio);
  }

  public PortfolioDetailResponse getPortfolio(Long spaceId, String id) {
    Portfolio portfolio = portfolioRepository.findByIdAndSpaceId(id, spaceId)
        .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND));
    return PortfolioDetailResponse.of(portfolio);
  }

  @Transactional
  public void createPortfolio(Long spaceId, Long userId, PortfolioCreateRequest request) {
    Portfolio portfolio = new Portfolio();

    portfolio.setSpaceId(spaceId);

    // Author 설정
    Author author = new Author();
    author.setId(request.getAuthor().getId());
    author.setNickname(request.getAuthor().getNickname());
    portfolio.setAuthor(author);

    // Duration 설정
    Duration duration = new Duration();
    duration.setStartDate(request.getDuration().getStartDate());
    duration.setEndDate(request.getDuration().getEndDate());
    portfolio.setDuration(duration);

    // Contents 설정
    Contents contents = new Contents();
    contents.setTechStack(request.getContents().getTechStack());
    contents.setSummary(request.getContents().getSummary());
    contents.setDescription(request.getContents().getDescription());
    contents.setFeatures(request.getContents().getFeatures());
    contents.setRoles(request.getContents().getRoles());

    Architecture architecture = new Architecture();
    architecture.setCommunication(request.getContents().getArchitecture().getCommunication());
    architecture.setDeployment(request.getContents().getArchitecture().getDeployment());
    contents.setArchitecture(architecture);

    portfolio.setContents(contents);

    // GitHub Repos 설정
    List<GitHubRepo> githubRepos = request.getGithubRepos().stream()
        .map(repo -> {
          GitHubRepo githubRepo = new GitHubRepo();
          githubRepo.setName(repo.getName());
          githubRepo.setUrl(repo.getUrl());
          githubRepo.setDescription(repo.getDescription());
          githubRepo.setLanguage(repo.getLanguage());
          githubRepo.setLineCount(repo.getLineCount());
          githubRepo.setByteSize(repo.getByteSize());
          githubRepo.setSelectedDirectories(repo.getSelectedDirectories());
          return githubRepo;
        })
        .collect(Collectors.toList());
    portfolio.setGithubRepos(githubRepos);

    // Saved Files 설정
    List<SavedFile> savedFiles = request.getSavedFiles().stream()
        .map(file -> {
          SavedFile savedFile = new SavedFile();
          savedFile.setId(file.getId());
          savedFile.setName(file.getName());
          savedFile.setPath(file.getPath());
          savedFile.setRepository(file.getRepository());
          savedFile.setSavedPath(file.getSavedPath());
          return savedFile;
        })
        .collect(Collectors.toList());
    portfolio.setSavedFiles(savedFiles);

    // 나머지 필드 설정
    portfolio.setTitle(request.getTitle());
    portfolio.setPublicAccess(request.isPublicAccess());
    portfolio.setGithubLink(request.getGithubLink());
    portfolio.setDeployLink(request.getDeployLink());
    portfolio.setMemberCount(request.getMemberCount());
    portfolio.setMemberRoles(request.getMemberRoles());

    // MongoDB에 저장
    portfolioRepository.save(portfolio);

  }

  @Transactional
  public PortfolioDetailResponse updatePortfolio(String portfolioId, Long userId, PortfolioPutRequest request) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND));

    if(!portfolio.getAuthor().getId().equals(userId)) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    // 기본 필드 업데이트
    portfolio.setTitle(request.getTitle());
    portfolio.setGithubLink(request.getGithubLink());
    portfolio.setDeployLink(request.getDeployLink());
    portfolio.setMemberCount(request.getMemberCount());
    portfolio.setMemberRoles(request.getMemberRoles());
    portfolio.setPublicAccess(request.isPublicAccess());

    // Duration 업데이트
    if (request.getDuration() != null) {
      Duration duration = Duration.builder()
          .startDate(request.getDuration().getStartDate())
          .endDate(request.getDuration().getEndDate())
          .build();
      portfolio.setDuration(duration);
    }

    // Contents 업데이트
    if (request.getContents() != null) {
      Architecture architecture = null;
      if (request.getContents().getArchitecture() != null) {
        architecture = Architecture.builder()
            .communication(request.getContents().getArchitecture().getCommunication())
            .deployment(request.getContents().getArchitecture().getDeployment())
            .build();
      }

      Contents contents = Contents.builder()
          .techStack(request.getContents().getTechStack())
          .summary(request.getContents().getSummary())
          .description(request.getContents().getDescription())
          .roles(request.getContents().getRoles())
          .features(request.getContents().getFeatures())
          .architecture(architecture)
          .build();
      portfolio.setContents(contents);
    }

    // GitHub Repos 업데이트
    portfolio.setGithubRepos(request.getGithubRepos() != null ?
        request.getGithubRepos().stream().map(repo ->
            GitHubRepo.builder()
                .name(repo.getName())
                .url(repo.getUrl())
                .description(repo.getDescription())
                .language(repo.getLanguage())
                .lineCount(repo.getLineCount())
                .byteSize(repo.getByteSize())
                .selectedDirectories(repo.getSelectedDirectories())
                .build()
        ).collect(Collectors.toList()) : List.of());

    // Saved Files 업데이트
    portfolio.setSavedFiles(request.getSavedFiles() != null ?
        request.getSavedFiles().stream().map(file ->
            SavedFile.builder()
                .id(file.getId())
                .name(file.getName())
                .path(file.getPath())
                .repository(file.getRepository())
                .savedPath(file.getSavedPath())
                .build()
        ).collect(Collectors.toList()) : List.of());

    // 저장 및 응답 반환
    Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
    return PortfolioDetailResponse.of(updatedPortfolio);
  }

  @Transactional
  public void deletePortfolio(String portfolioId, Long userId) {
    Portfolio portfolio = portfolioRepository.findById(portfolioId)
        .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_NOT_FOUND));

    if (!portfolio.getAuthor().getId().equals(userId)) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    portfolioRepository.delete(portfolio);
  }
}
