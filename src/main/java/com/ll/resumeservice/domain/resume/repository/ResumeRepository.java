package com.ll.resumeservice.domain.resume.repository;

import com.ll.resumeservice.domain.resume.document.Resume;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends MongoRepository<Resume, String> {

}
