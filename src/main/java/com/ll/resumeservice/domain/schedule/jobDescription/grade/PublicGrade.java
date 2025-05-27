package com.ll.resumeservice.domain.schedule.jobDescription.grade;

public enum PublicGrade {
  PUBLIC("전체"),
  GROUP("그룹"),
  PRIVATE("개인");

  private final String displayName;

  PublicGrade(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}