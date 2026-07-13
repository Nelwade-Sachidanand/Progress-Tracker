package com.novillex.progresstracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.novillex.progresstracker.entity.Documents;

@Repository
public interface DocumentRepository extends MongoRepository<Documents, String> {

	Optional<Documents> findByProjectIdAndPhaseIdAndMilestoneIdAndTaskIdAndSubTaskIdAndActivityId(
	        String projectId,
	        String phaseId,
	        String milestoneId,
	        String taskId,
	        String subTaskId,
	        String activityId);

	Optional<Documents> findByDocumentsDocumentId(String documentId);
	
	List<Documents> findByProjectId(String projectId);

}