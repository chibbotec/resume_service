package com.ll.resumeservice.domain.portfolio.portfolio.dto;


import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioInfo {
  private String id;
  private String title;
  private String summary;
  private LocalDateTime updatedAt;

  private static PortfolioInfo of(Portfolio portfolio){
    return PortfolioInfo.builder()
        .id(portfolio.getId())
        .title(portfolio.getTitle())
        .summary(portfolio.getContents().getSummary())
        .updatedAt(portfolio.getUpdatedAt())
        .build();
  }
}
