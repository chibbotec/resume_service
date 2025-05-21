package com.ll.resumeservice.domain.portfolio.github.repository;

import com.ll.resumeservice.domain.portfolio.github.document.GithubCommit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GithubCommitRepository extends MongoRepository<GithubCommit, String> {
    Optional<GithubCommit> findByUserIdAndRepository(Long userId, String repository);
    void deleteByUserIdAndRepository(Long userId, String repository);
    boolean existsByUserIdAndRepository(Long userId, String repository);
}
