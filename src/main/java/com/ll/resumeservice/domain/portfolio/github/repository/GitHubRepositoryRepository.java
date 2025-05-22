package com.ll.resumeservice.domain.portfolio.github.repository;

import com.ll.resumeservice.domain.portfolio.github.document.GitHubRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GitHubRepositoryRepository extends MongoRepository<GitHubRepository, String> {

  List<GitHubRepository> findByUserId(Long userId);

  Optional<GitHubRepository> findByUserIdAndFullName(Long userId, String fullName);

}
