package com.novillex.progresstracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.ActivityUpdateRequest;


public interface ActivityUpdateRequestRepository extends MongoRepository<ActivityUpdateRequest, String> {

	Optional<ActivityUpdateRequest> findByProjectIdAndPhaseNameAndMilestoneNameAndTaskNameAndSubTaskNameAndActivityNameAndStatus(
			String projectId, String phaseName, String milestoneName, String taskName, String subTaskName,
			String activityName, String status);

	List<ActivityUpdateRequest> findByStatus(String status);

	Optional<ActivityUpdateRequest> findById(String id);
}
