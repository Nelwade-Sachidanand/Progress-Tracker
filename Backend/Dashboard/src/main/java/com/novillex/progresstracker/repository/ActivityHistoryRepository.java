package com.novillex.progresstracker.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.novillex.progresstracker.entity.ActivityHistory;

@Repository
public interface ActivityHistoryRepository
        extends MongoRepository<ActivityHistory, String> {

    List<ActivityHistory> findByProjectId(String projectId);

}