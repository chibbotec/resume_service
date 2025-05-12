package com.ll.resumeservice.global.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
  private Long id;
  private String username;
  private String email;
  private String nickname;
}
