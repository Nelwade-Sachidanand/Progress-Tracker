package com.novillex.progresstracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.Project;

public interface ProjectRepository extends MongoRepository<Project, String> {

	Optional<Project> findByProjectName(String projectName);

	List<Project> findByIdIn(List<String> ids);

	void deleteByProjectName(String projectName);

	Optional<Project> findByProjectInformationId(String projectInformationId);
	
	Optional<Project> findByPhasesMilestonesTasksSubTasksActivitiesActivityId(String activityId);
}