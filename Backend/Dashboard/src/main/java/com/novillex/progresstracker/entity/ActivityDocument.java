package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDocument {

    private String documentId;
    private String fileName;
    private String filePath;

    private String uploadedBy;
    private LocalDateTime uploadedDate;
}
