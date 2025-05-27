package com.ll.resumeservice.domain.schedule.jobDescription.repository;

import com.ll.resumeservice.domain.schedule.jobDescription.document.JobDescription;
import com.ll.resumeservice.domain.schedule.jobDescription.grade.PublicGrade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDescriptionRepository extends MongoRepository<JobDescription, Long> {

  List<JobDescription> findBySpaceId(Long spaceId);

  Optional<JobDescription> findByIdAndSpaceId(String id, Long spaceId);

  List<JobDescription> findByPublicGrade(PublicGrade publicGrade);

  @Query("{ $or: [ " +
      "{ 'publicGrade': 'PUBLIC' }, " +
      "{ 'spaceId': ?0, 'publicGrade': 'GROUP' }, " +
      "{ 'userId': ?1, 'publicGrade': 'PRIVATE' } " +
      "] }")
  List<JobDescription> findJobDescriptionsByAccess(Long spaceId, Long userId);
}
