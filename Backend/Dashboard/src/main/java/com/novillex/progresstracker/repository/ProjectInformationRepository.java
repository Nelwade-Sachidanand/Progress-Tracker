package com.novillex.progresstracker.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.novillex.progresstracker.entity.ProjectInformation;

@Repository
public interface ProjectInformationRepository
        extends MongoRepository<ProjectInformation, String> {

    Optional<ProjectInformation> findByProjectName(String projectName);
}