package com.ll.resumeservice.domain.portfolio.portfolio.dto.response;

import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Author;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Contents;
import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio.Duration;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Builder
public class PortfolioResponse {
  private String id;

  private Long spaceId;

  private String title;

  private Author author;

  private Duration duration;

  private Contents contents;

  public static PortfolioResponse of(Portfolio portfolio) {
    return PortfolioResponse.builder()
        .id(portfolio.getId())
        .spaceId(portfolio.getSpaceId())
        .title(portfolio.getTitle())
        .author(portfolio.getAuthor())
        .duration(portfolio.getDuration())
        .contents(portfolio.getContents())
        .build();
  }
}
