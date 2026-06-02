package com.dashboard.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.dashboard.entity.AuditLog;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

	List<AuditLog> findByProjectName(String projectName);

}