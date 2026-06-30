package com.novillex.progresstracker.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.novillex.progresstracker.entity.Notification;
import org.springframework.data.domain.Sort;

public interface NotificationRepository extends MongoRepository<Notification, String> {

	long countByRecipientUserIdAndReadFalse(String recipientUserId);
	
	long countByRecipientUserIdIsNullAndReadFalse();

	List<Notification> findByRecipientUserIdAndReadFalse(String recipientUserId, Sort sort);
	
	List<Notification> findByRecipientUserIdIsNullAndReadFalse(Sort sort);
	
}
