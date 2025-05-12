package com.ll.resumeservice.domain.portfolio.portfolio.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SummarizerService {

  private final ChatLanguageModel model;

  /**
   * 레포지토리 내 모든 파일을 재귀적으로 가져옵니다.
   */
  public List<Map<String, Object>> getRepositoryFiles(GHRepository repository, String path, String branch) throws IOException {
    List<Map<String, Object>> result = new ArrayList<>();
    List<GHContent> contents = repository.getDirectoryContent(path, branch);

    for (GHContent content : contents) {
      Map<String, Object> item = new HashMap<>();
      item.put("name", content.getName());
      item.put("path", content.getPath());
      item.put("type", content.isDirectory() ? "directory" : "file");
      item.put("size", content.getSize());

      if (content.isDirectory()) {
        item.put("children", getRepositoryFiles(repository, content.getPath(), branch));
      }

      result.add(item);
    }

    return result;
  }

  /**
   * 선택된 파일들을 요약합니다.
   */
  public Map<String, String> summarizeFiles(GHRepository repository, List<String> filePaths, String branch) {
    Map<String, String> summaries = new HashMap<>();

    for (String path : filePaths) {
      try {
        GHContent content = repository.getFileContent(path, branch);
        String fileContent = new String(content.read().readAllBytes(), StandardCharsets.UTF_8);

        // 파일이 너무 크면 잘라냅니다
        if (fileContent.length() > 10000) {
          fileContent = fileContent.substring(0, 10000) + "... (truncated)";
        }

        String summary = summarizeWithAI(path, fileContent);
        summaries.put(path, summary);
      } catch (IOException e) {
        summaries.put(path, "파일을 읽는 중 오류 발생: " + e.getMessage());
      }
    }

    return summaries;
  }

  /**
   * LangChain을 통해 AI에게 파일 요약을 요청합니다.
   */
  private String summarizeWithAI(String filePath, String content) {
    String prompt = "다음은 GitHub 레포지토리의 '" + filePath + "' 파일 내용입니다. 이 파일을 간결하게 요약해주세요:\n\n" + content;

    try {
      List<ChatMessage> messages = new ArrayList<>();
      messages.add(new SystemMessage("당신은 코드와 문서를 이해하고 요약하는 전문가입니다. 파일의 주요 내용, 목적, 기능을 간결하게 요약해주세요."));
      messages.add(new UserMessage(prompt));

      AiMessage response = model.generate(messages).content();
      return response.text();
    } catch (Exception e) {
      e.printStackTrace();
      return "요약 실패: " + e.getMessage();
    }
  }
}