package com.ll.resumeservice.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // CSRF 보호 비활성화
        .cors(cors -> cors.disable())  // CORS 제한 비활성화
        .authorizeHttpRequests(authorize -> authorize
            .anyRequest().permitAll()  // 모든 요청 허용
        )
        .oauth2Login(oauth2 -> oauth2
            .defaultSuccessUrl("/dashboard", true)
        )
        .logout(logout -> logout
            .logoutSuccessUrl("/")
        );

    return http.build();
  }
}