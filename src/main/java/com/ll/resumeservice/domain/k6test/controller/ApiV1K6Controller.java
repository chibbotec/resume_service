package com.ll.resumeservice.domain.k6test.controller;


import com.ll.resumeservice.domain.k6test.service.K6Service;
import com.ll.resumeservice.domain.portfolio.github.dto.request.SaveRepositoryRequest;
import com.ll.resumeservice.domain.portfolio.github.dto.response.RepoTaskStatusResponse;
import com.ll.resumeservice.domain.portfolio.github.dto.response.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume/{spaceId}/k6")
@RequiredArgsConstructor
public class ApiV1K6Controller {

  private final K6Service k6Service;

  @GetMapping("/tasks/{taskId}")
  public ResponseEntity<RepoTaskStatusResponse> getTaskStatus(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("taskId") String taskId
  ) {
    RepoTaskStatusResponse status = k6Service.getTaskStatus(taskId);

    if (status == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(status);
  }

  @PostMapping("/users/{userId}/save-files/async")
  public ResponseEntity<TaskResponse> saveFilesAsync(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("userId") Long userId,
      @RequestBody SaveRepositoryRequest saveRepositoryRequest
  ) {
    // 비동기 다운로드 작업 시작 및 태스크 ID 반환
    String taskId = k6Service.AsyncRepositoryDownload(
        spaceId, userId, saveRepositoryRequest);

    // 즉시 응답 반환
    TaskResponse response = TaskResponse.builder()
        .taskId(taskId)
        .message("다운로드가 백그라운드에서 시작되었습니다. 상태를 확인하려면 '/api/spaces/" +
            spaceId + "/github/tasks/" + taskId + "' 엔드포인트를 사용하세요.")
        .build();

    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/users/{userId}/save-files/serial")
  public ResponseEntity<TaskResponse> saveFilesSerial(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("userId") Long userId,
      @RequestBody SaveRepositoryRequest saveRepositoryRequest
  ){
    String taskId = k6Service.serailRepositoryDownload(
        spaceId, userId, saveRepositoryRequest);

    // 즉시 응답 반환
    TaskResponse response = TaskResponse.builder()
        .taskId(taskId)
        .message("다운로드가 백그라운드에서 시작되었습니다. 상태를 확인하려면 '/api/spaces/" +
            spaceId + "/github/tasks/" + taskId + "' 엔드포인트를 사용하세요.")
        .build();

    return ResponseEntity.accepted().body(response);
  }

  @PostMapping("/users/{userId}/save-files/zip")
  public ResponseEntity<TaskResponse> saveFilesZip(
      @PathVariable("spaceId") Long spaceId,
      @PathVariable("userId") Long userId,
      @RequestBody SaveRepositoryRequest saveRepositoryRequest
  ){
    String taskId = k6Service.zipRepositoryDownload(spaceId, userId, saveRepositoryRequest);

    // 즉시 응답 반환
    TaskResponse response = TaskResponse.builder()
        .taskId(taskId)
        .message("다운로드가 백그라운드에서 시작되었습니다. 상태를 확인하려면 '/api/spaces/" +
            spaceId + "/github/tasks/" + taskId + "' 엔드포인트를 사용하세요.")
        .build();

    return ResponseEntity.accepted().body(response);
  }


}
