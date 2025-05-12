//package com.ll.resumeservice.domain.portfolio.github.controller;
//
//import com.ll.resumeservice.domain.portfolio.github.service.GitHubService;
//import com.ll.resumeservice.domain.portfolio.service.SummarizerService;
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import lombok.RequiredArgsConstructor;
//import org.kohsuke.github.GHRepository;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.ui.Model;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//@Controller
//@RequiredArgsConstructor
//public class ApiV1SummaryController {
//
//  private final GitHubService gitHubService;
//  private final SummarizerService summarizerService;
//
//  @GetMapping("/")
//  public String home() {
//    return "index";
//  }
//
//  @GetMapping("/dashboard")
//  public String dashboard(@AuthenticationPrincipal OAuth2User principal,
//      Model model,
//      OAuth2AuthenticationToken authentication) throws IOException {
//    model.addAttribute("user", principal.getAttribute("login"));
//    model.addAttribute("name", principal.getAttribute("name"));
//    model.addAttribute("repositories", gitHubService.getUserRepositories(authentication));
//    return "dashboard";
//  }
//
//  @GetMapping("/repository/{owner}/{repo}")
//  public String repository(@PathVariable String owner,
//      @PathVariable String repo,
//      @RequestParam(defaultValue = "main") String branch,
//      Model model,
//      OAuth2AuthenticationToken authentication) throws IOException {
//    String repoFullName = owner + "/" + repo;
//    GHRepository repository = gitHubService.getRepository(authentication, repoFullName);
//
//    model.addAttribute("repoName", repoFullName);
//    model.addAttribute("branch", branch);
//    model.addAttribute("files", summarizerService.getRepositoryFiles(repository, "", branch));
//
//    return "repository";
//  }
//
//  @PostMapping("/summarize")
//  @ResponseBody
//  public Map<String, String> summarize(@RequestBody Map<String, Object> request,
//      OAuth2AuthenticationToken authentication) throws IOException {
//    String repoFullName = (String) request.get("repository");
//    String branch = (String) request.get("branch");
//    @SuppressWarnings("unchecked")
//    List<String> filePaths = (List<String>) request.get("files");
//
//    GHRepository repository = gitHubService.getRepository(authentication, repoFullName);
//    return summarizerService.summarizeFiles(repository, filePaths, branch);
//  }
//}