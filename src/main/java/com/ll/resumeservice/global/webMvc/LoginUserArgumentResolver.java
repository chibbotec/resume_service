package com.ll.resumeservice.global.webMvc;


import com.ll.resumeservice.global.client.MemberResponse;
import com.ll.resumeservice.global.client.MemberServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

  private final MemberServiceClient memberServiceClient;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(LoginUser.class) &&
        parameter.getParameterType().equals(MemberResponse.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory
  ) {

    String username = webRequest.getHeader("X-Username");

    if (username == null) {
      log.debug("X-Username header not found");
      return null;
    }

    try {
      return memberServiceClient.getMemberByUsername(username);
    } catch (NumberFormatException e) {
      log.error("Invalid userId format: {}", username, e);
      return null;
    }
  }
}