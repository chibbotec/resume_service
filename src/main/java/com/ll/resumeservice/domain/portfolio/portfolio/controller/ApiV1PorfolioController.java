package com.ll.resumeservice.domain.portfolio.portfolio.controller;

import com.ll.resumeservice.domain.portfolio.portfolio.dto.request.PortfolioCreateRequest;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.request.PortfolioPutRequest;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.response.CatergorizedPortfolioResponse;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.response.PortfolioDetailResponse;
import com.ll.resumeservice.domain.portfolio.portfolio.dto.response.PortfolioResponse;
import com.ll.resumeservice.domain.portfolio.portfolio.service.PortfolioService;
import com.ll.resumeservice.global.client.MemberResponse;
import com.ll.resumeservice.global.webMvc.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resume/{spaceId}/portfolio")
@RequiredArgsConstructor
@Slf4j
public class ApiV1PorfolioController {

  private final PortfolioService portfolioService;

  @GetMapping("")
  public ResponseEntity<CatergorizedPortfolioResponse> getPortfolioList(
      @PathVariable("spaceId") Long spaceId,
      @LoginUser MemberResponse loginUser
  ){
    return ResponseEntity.ok(portfolioService.getPortfolioList(spaceId, loginUser.getId()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<PortfolioDetailResponse> getPortfolio(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("id") String id
  ){
    return ResponseEntity.ok(portfolioService.getPortfolio(spaceId, id));
  }

  @PostMapping
  public ResponseEntity<PortfolioResponse> createPortfolio(
      @PathVariable ("spaceId") Long spaceId,
      @LoginUser MemberResponse loginUser,
      @RequestBody PortfolioCreateRequest request
  ){
    portfolioService.createPortfolio(spaceId, loginUser.getId(), request);
    return ResponseEntity.ok().build();
  }

  @PutMapping("/{id}")
  public ResponseEntity<PortfolioDetailResponse> updatePortfolio(
      @PathVariable("id") String id,
      @LoginUser MemberResponse loginUser,
      @RequestBody PortfolioPutRequest request
  ){
    return ResponseEntity.ok(portfolioService.updatePortfolio( id, loginUser.getId(), request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity deletePortfolio(
      @PathVariable("id") String id,
      @LoginUser MemberResponse loginUser){
    portfolioService.deletePortfolio(id, loginUser.getId());

    return ResponseEntity.noContent().build();
  }

}
