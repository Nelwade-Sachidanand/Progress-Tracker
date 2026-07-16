package com.novillex.progresstracker.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullname;

    private String username;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String role;

    private List<String> projectIds;

    private Boolean status;
    
    private Boolean passwordResetRequested = false;

    private Boolean temporaryPasswordActive = false;

    private Boolean forcePasswordChange = false;
}