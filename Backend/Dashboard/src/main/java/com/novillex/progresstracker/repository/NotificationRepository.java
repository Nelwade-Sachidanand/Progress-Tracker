package com.novillex.progresstracker.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

	long countByReadFalse();

}
