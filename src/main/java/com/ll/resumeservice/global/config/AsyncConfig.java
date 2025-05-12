package com.ll.resumeservice.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "githubTaskExecutor")
  public Executor githubTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);  // 기본 스레드 풀 크기
    executor.setMaxPoolSize(10);  // 최대 스레드 풀 크기
    executor.setQueueCapacity(25); // 대기 큐 크기
    executor.setThreadNamePrefix("GitHub-Async-");
    executor.initialize();
    return executor;
  }
}