package com.ll.resumeservice.domain.resume.document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Builder
@Document(collection = "resumes")
public class Resume {

}
