package com.dashboard.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dashboard.entity.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {

	Optional<Project> findByProjectName(String projectName);
}