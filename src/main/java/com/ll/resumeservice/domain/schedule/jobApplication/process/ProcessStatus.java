package com.ll.resumeservice.domain.schedule.jobApplication.process;

public enum ProcessStatus {
  RESUME("지원서"),
  DOCUMENT("서류"),
  CODING("코딩테스트"),
  INTERVIEW1("1차면접"),
  INTERVIEW2("2차면접"),
  PASS("합격"),
  FAIL("불합격");

  private final String displayName;

  ProcessStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}