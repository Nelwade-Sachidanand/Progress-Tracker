package com.novillex.progresstracker.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.novillex.progresstracker.entity.Documents;

@Repository
public interface DocumentRepository extends MongoRepository<Documents, String> {

	Optional<Documents> findByProjectNameAndBankNameAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityName(
			String projectName, String bankName, String phaseName, String milestoneName, String taskName, String subTaskName,
			String activityName);

	Optional<Documents> findByDocumentsDocumentId(String documentId);

}