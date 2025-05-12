package com.ll.resumeservice.domain.portfolio.portfolio.dto;

import com.ll.resumeservice.domain.portfolio.portfolio.document.Portfolio;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatergorizedPortfolioResponse {
  private List<PortfolioResponse> publicPortfolios;
  private List<PortfolioResponse> privatePortfolios;

  public static CatergorizedPortfolioResponse of(
      List<Portfolio> publicPortfolios,
      List<Portfolio> privatePortfolios) {
    return CatergorizedPortfolioResponse.builder()
        .publicPortfolios(publicPortfolios.stream().map(PortfolioResponse::of).toList())
        .privatePortfolios(privatePortfolios.stream().map(PortfolioResponse::of).toList())
        .build();
  }
}
