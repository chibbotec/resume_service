package com.ll.resumeservice.domain.portfolio.github.service;


import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import com.ll.resumeservice.domain.portfolio.github.repository.GitHubRepositoryMongo;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class GitHubMongoService {
//
  private final GitHubRepositoryMongo gitHubRepositoryMongo;

  @Transactional
  public GitHubRepository saveRepositoryToMongo(Long userId, Map<String, Object> repoDetail) {
    try {
      // 기존 데이터가 있는지 확인 (repoId로 검색)
      Long repoId = ((Number) repoDetail.get("id")).longValue();
      Optional<GitHubRepository> existingRepo = gitHubRepositoryMongo.findByUserIdAndRepoId(userId, repoId);

      GitHubRepository repo;
      if (existingRepo.isPresent()) {
        // 기존 데이터 업데이트
        repo = existingRepo.get();
        updateRepoFields(repo, repoDetail);
      } else {
        // 새 데이터 생성
        repo = new GitHubRepository();
        repo.setUserId(userId);
        repo.setRepoId(repoId);
        updateRepoFields(repo, repoDetail);
      }

      // 저장 시간 업데이트
      repo.setSavedAt(new Date());

      // MongoDB에 저장
      return gitHubRepositoryMongo.save(repo);
    } catch (Exception e) {
      log.error("GitHub 레포지토리 정보를 MongoDB에 저장하는 중 오류 발생", e);
      throw new RuntimeException("GitHub 레포지토리 정보를 MongoDB에 저장하는데 실패했습니다", e);
    }
  }

  private void updateRepoFields(GitHubRepository repo, Map<String, Object> repoDetail) {
    repo.setName((String) repoDetail.get("name"));
    repo.setFullName((String) repoDetail.get("fullName"));
    repo.setDescription((String) repoDetail.get("description"));

    // URL 객체 처리 - toString() 메서드 사용
    Object urlObj = repoDetail.get("url");
    repo.setUrl(urlObj instanceof URL ? ((URL) urlObj).toString() : (String) urlObj);

    Object apiUrlObj = repoDetail.get("apiUrl");
    repo.setApiUrl(apiUrlObj instanceof URL ? ((URL) apiUrlObj).toString() : (String) apiUrlObj);

    Object gitUrlObj = repoDetail.get("gitUrl");
    repo.setGitUrl(gitUrlObj instanceof URL ? ((URL) gitUrlObj).toString() : (String) gitUrlObj);

    Object sshUrlObj = repoDetail.get("sshUrl");
    repo.setSshUrl(sshUrlObj instanceof URL ? ((URL) sshUrlObj).toString() : (String) sshUrlObj);

    // 나머지 필드들...
    repo.setHomepage((String) repoDetail.get("homepage"));
    repo.setLanguage((String) repoDetail.get("language"));
    repo.setDefaultBranch((String) repoDetail.get("defaultBranch"));
    repo.setIsPrivate((Boolean) repoDetail.get("private"));
    repo.setIsFork((Boolean) repoDetail.get("fork"));
    repo.setIsArchived((Boolean) repoDetail.get("archived"));
    repo.setIsDisabled((Boolean) repoDetail.get("disabled"));
    repo.setStars(((Number) repoDetail.get("stars")).intValue());
    repo.setWatchers(((Number) repoDetail.get("watchers")).intValue());
    repo.setForks(((Number) repoDetail.get("forks")).intValue());
    repo.setSize(((Number) repoDetail.get("size")).intValue());
    repo.setCreatedAt((Date) repoDetail.get("createdAt"));
    repo.setUpdatedAt((Date) repoDetail.get("updatedAt"));
    repo.setPushedAt((Date) repoDetail.get("pushedAt"));
    repo.setFiles((List<Map<String, Object>>) repoDetail.get("files"));
  }

  public List<Map<String, Object>> getRepositoriesFromMongo(Long userId) {
    try {
      List<GitHubRepository> repositories = gitHubRepositoryMongo.findByUserId(userId);
      return repositories.stream().map(repo -> {
        Map<String, Object> repoMap = new HashMap<>();
        repoMap.put("id", repo.getRepoId());
        repoMap.put("name", repo.getName());
        repoMap.put("fullName", repo.getFullName());
        repoMap.put("description", repo.getDescription() != null ? repo.getDescription() : "");
        repoMap.put("url", repo.getUrl());
        repoMap.put("language", repo.getLanguage());
        repoMap.put("defaultBranch", repo.getDefaultBranch());
        repoMap.put("fork", repo.getIsFork());
        repoMap.put("stars", repo.getStars());
        repoMap.put("forks", repo.getForks());
        repoMap.put("size", repo.getSize());
        repoMap.put("savedAt", repo.getSavedAt());
        return repoMap;
      }).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("MongoDB에서 GitHub 레포지토리 목록 조회 중 오류 발생", e);
      throw new RuntimeException("MongoDB에서 GitHub 레포지토리 목록을 가져오는데 실패했습니다", e);
    }
  }

  public Map<String, Object> getRepositoryDetailFromMongo(Long userId, String fullRepoName) {
    try {
      GitHubRepository repo = gitHubRepositoryMongo.findByUserIdAndFullName(userId, fullRepoName);
      if (repo == null) {
        throw new RuntimeException("레포지토리를 찾을 수 없습니다: " + fullRepoName);
      }

      Map<String, Object> repoDetail = new HashMap<>();
      repoDetail.put("id", repo.getRepoId());
      repoDetail.put("name", repo.getName());
      repoDetail.put("fullName", repo.getFullName());
      repoDetail.put("description", repo.getDescription() != null ? repo.getDescription() : "");
      repoDetail.put("url", repo.getUrl());
      repoDetail.put("apiUrl", repo.getApiUrl());
      repoDetail.put("gitUrl", repo.getGitUrl());
      repoDetail.put("sshUrl", repo.getSshUrl());
      repoDetail.put("homepage", repo.getHomepage());
      repoDetail.put("language", repo.getLanguage());
      repoDetail.put("defaultBranch", repo.getDefaultBranch());
      repoDetail.put("private", repo.getIsPrivate());
      repoDetail.put("fork", repo.getIsFork());
      repoDetail.put("archived", repo.getIsArchived());
      repoDetail.put("disabled", repo.getIsDisabled());
      repoDetail.put("stars", repo.getStars());
      repoDetail.put("watchers", repo.getWatchers());
      repoDetail.put("forks", repo.getForks());
      repoDetail.put("size", repo.getSize());
      repoDetail.put("files", repo.getFiles());
      repoDetail.put("savedAt", repo.getSavedAt());

      return repoDetail;
    } catch (Exception e) {
      log.error("MongoDB에서 레포지토리 상세 정보 조회 중 오류 발생", e);
      throw new RuntimeException("MongoDB에서 레포지토리 상세 정보를 가져오는데 실패했습니다: " + fullRepoName, e);
    }
  }
//
//  /**
//   * 레포지토리 컨텐츠를 MongoDB에서 가져오기
//   * 특정 경로의 파일 및 디렉토리 목록을 반환
//   */
//  public List<Map<String, Object>> getRepositoryContentsFromMongo(Long userId, String fullRepoName, String path) {
//    try {
//      GitHubRepository repo = gitHubRepositoryMongo.findByUserIdAndFullName(userId, fullRepoName);
//      if (repo == null) {
//        throw new RuntimeException("레포지토리를 찾을 수 없습니다: " + fullRepoName);
//      }
//
//      // 레포지토리의 파일 목록
//      List<Map<String, Object>> files = repo.getFiles();
//      if (files == null || files.isEmpty()) {
//        return new ArrayList<>();
//      }
//
//      // 요청된 경로가 없으면 루트 디렉토리 내용 반환
//      if (path == null || path.trim().isEmpty()) {
//        return files;
//      }
//
//      // 요청된 경로에 맞는 디렉토리 찾기
//      Map<String, Object> directoryContent = findDirectoryByPath(files, path);
//
//      if (directoryContent != null && directoryContent.containsKey("directory")) {
//        return (List<Map<String, Object>>) directoryContent.get("directory");
//      }
//
//      return new ArrayList<>();
//    } catch (Exception e) {
//      log.error("MongoDB에서 레포지토리 컨텐츠 조회 중 오류 발생", e);
//      throw new RuntimeException("MongoDB에서 레포지토리 컨텐츠를 가져오는데 실패했습니다: " + fullRepoName + ", 경로: " + path, e);
//    }
//  }
//
//  /**
//   * 특정 경로의 디렉토리 찾기
//   */
//  private Map<String, Object> findDirectoryByPath(List<Map<String, Object>> files, String targetPath) {
//    // 경로 정규화 (앞뒤 슬래시 제거)
//    targetPath = targetPath.replaceAll("^/+", "").replaceAll("/+$", "");
//
//    // 현재 레벨에서 찾기
//    for (Map<String, Object> file : files) {
//      String path = (String) file.get("path");
//      if (path.equals(targetPath) && "dir".equals(file.get("type"))) {
//        return file;
//      }
//    }
//
//    // 하위 디렉토리 탐색
//    for (Map<String, Object> file : files) {
//      if ("dir".equals(file.get("type")) && file.containsKey("directory")) {
//        List<Map<String, Object>> subDir = (List<Map<String, Object>>) file.get("directory");
//        Map<String, Object> found = findDirectoryByPath(subDir, targetPath);
//        if (found != null) {
//          return found;
//        }
//      }
//    }
//
//    return null;
//  }
}
