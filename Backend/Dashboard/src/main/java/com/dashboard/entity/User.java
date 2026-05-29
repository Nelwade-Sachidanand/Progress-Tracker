package com.dashboard.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;

    private String password;

    private String role;

    private List<String> projectNames;

    private Boolean active;
}