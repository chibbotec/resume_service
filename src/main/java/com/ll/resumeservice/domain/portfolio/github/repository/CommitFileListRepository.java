package com.ll.resumeservice.domain.portfolio.github.repository;

import com.ll.resumeservice.domain.portfolio.github.document.CommitFileList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitFileListRepository extends MongoRepository<CommitFileList, String> {

  // 사용자ID와 레포지토리로 파일 목록 조회
  Optional<CommitFileList> findByUserIdAndRepository(Long userId, String repository);

  // 사용자ID와 레포지토리로 데이터 삭제
  void deleteByUserIdAndRepository(Long userId, String repository);

  // 특정 사용자의 모든 레포지토리 파일 목록 조회
  java.util.List<CommitFileList> findByUserId(Long userId);

  // 특정 사용자의 데이터 존재 여부 확인
  boolean existsByUserIdAndRepository(Long userId, String repository);
}