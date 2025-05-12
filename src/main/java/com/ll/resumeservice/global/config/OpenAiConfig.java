package com.ll.resumeservice.global.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

  @Value("${app.openai.api-key}")
  private String apiKey;

  @Bean
  public ChatLanguageModel chatLanguageModel() {
    return OpenAiChatModel.builder()
        .apiKey(apiKey)
        .modelName("gpt-4")
        .temperature(0.7)
        .build();
  }
}