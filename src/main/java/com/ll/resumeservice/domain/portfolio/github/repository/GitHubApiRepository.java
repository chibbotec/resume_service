package com.ll.resumeservice.domain.portfolio.github.repository;

import com.ll.resumeservice.domain.portfolio.github.entity.GitHubApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubApiRepository extends JpaRepository<GitHubApi, Long> {

  GitHubApi findByUserId(Long userId);
}
