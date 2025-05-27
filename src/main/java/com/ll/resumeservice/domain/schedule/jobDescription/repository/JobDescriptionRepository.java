package com.ll.resumeservice.domain.schedule.jobDescription.repository;

import com.ll.resumeservice.domain.schedule.jobDescription.document.JobDescription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDescriptionRepository extends MongoRepository<JobDescription, Long> {

  List<JobDescription> findBySpaceId(Long spaceId);

  Optional<JobDescription> findByIdAndSpaceId(String id, Long spaceId);
}
