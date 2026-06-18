package com.novillex.progresstracker.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;
    
    private String fullname;

    private String username;

    private String password;

    private String role;

    private List<String> projectIds;

    private Boolean status;
}