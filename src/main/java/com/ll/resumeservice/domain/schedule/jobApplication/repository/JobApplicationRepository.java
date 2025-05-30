package com.ll.resumeservice.domain.schedule.jobApplication.repository;

import com.ll.resumeservice.domain.schedule.jobApplication.document.JobApplication;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {

  List<JobApplication> findBySpaceIdAndUserId(Long spaceId, Long id);
}
