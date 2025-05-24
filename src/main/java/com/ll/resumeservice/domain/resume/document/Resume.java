package com.ll.resumeservice.domain.resume.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
  @Id
  private String id;

  @Field("space_id")
  private Long spaceId;

  @Field("author")
  private Author author;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Field("title")
  private String title;

  @Field("name")
  private String name;

  @Field("email")
  private String email;

  @Field("phone")
  private String phone;

  @Field("career_type")
  private String careerType;

  @Field("position")
  private String position;

  @Field("tech_stack")
  private List<String> techStack;

  @Field("tech_summary")
  private String techSummary;

  @Field("links")
  private List<Link> links;

  @Field("careers")
  private List<Career> careers;

  @Field("projects")
  private List<Project> projects;

  @Field("educations")
  private List<Education> educations;

  @Field("certificates")
  private List<Certificate> certificates;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Author {
    @Field("id")
    private Long id;

    @Field("nickname")
    private String nickname;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Link {
    @Field("type")
    private String type;

    @Field("url")
    private String url;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Career {
    @Field("period")
    private String period;

    @Field("company")
    private String company;

    @Field("position")
    private String position;

    @Field("is_current")
    private Boolean isCurrent;

    @Field("start_date")
    private LocalDate startDate;

    @Field("end_date")
    private LocalDate endDate;

    @Field("description")
    private String description;

    @Field("achievement")
    private String achievement;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Project {
    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Field("tech_stack")
    private List<String> techStack;

    @Field("role")
    private String role;

    @Field("start_date")
    private LocalDate startDate;

    @Field("end_date")
    private LocalDate endDate;

    @Field("member_count")
    private Integer memberCount;

    @Field("member_role")
    private String memberRole;

    @Field("github_link")
    private String githubLink;

    @Field("deploy_link")
    private String deployLink;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Education {
    @Field("school")
    private String school;

    @Field("major")
    private String major;

    @Field("start_date")
    private LocalDate startDate;

    @Field("end_date")
    private LocalDate endDate;

    @Field("degree")
    private String degree;

    @Field("note")
    private String note;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Certificate {
    @Field("type")
    private String type;

    @Field("name")
    private String name;

    @Field("date")
    private LocalDate date;

    @Field("organization")
    private String organization;
  }
}
