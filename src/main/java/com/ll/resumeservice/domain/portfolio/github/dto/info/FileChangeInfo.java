package com.ll.resumeservice.domain.portfolio.github.dto.info;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileChangeInfo {
    private String sha;
    private String filename;
    private String status;
    private int additions;
    private int deletions;
    private int changes;
    private String patch;
    private String blobUrl;
}
