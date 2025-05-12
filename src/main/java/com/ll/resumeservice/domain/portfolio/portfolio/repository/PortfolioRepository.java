package com.ll.resumeservice.domain.portfolio.portfolio.repository;

import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends MongoRepository<Portfolio, String> {

  List<Portfolio> findAllBySpaceId(Long spaceId);

  Optional<Portfolio> findByIdAndSpaceId(String id, Long spaceId);

  List<Portfolio> findAllBySpaceIdAndPublicAccess(Long spaceId, boolean b);

  List<Portfolio> findAllBySpaceIdAndAuthor_Id(Long spaceId, Long memberId);
}
