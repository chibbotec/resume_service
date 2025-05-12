package com.ll.resumeservice.domain.portfolio.github.repository;

import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GitHubRepositoryMongo extends MongoRepository<GitHubRepository, String> {
  List<GitHubRepository> findByUserId(Long userId);
  GitHubRepository findByUserIdAndFullName(Long userId, String fullName);

  void deleteByUserId(Long userId);

  void deleteByUserIdAndRepoId(Long userId, Long repoId);

  Optional<GitHubRepository> findByUserIdAndRepoId(Long userId, Long repoId);
}
