package com.novillex.progresstracker.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {

	Optional<Project> findByProjectName(String projectName);
	
	void deleteByProjectName(String projectName);
}