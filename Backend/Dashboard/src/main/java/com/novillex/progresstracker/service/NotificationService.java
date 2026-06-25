package com.novillex.progresstracker.service;

public interface NotificationService {

	void createNotification(String title, String message, String type, String referenceId, String redirectUrl,
			String recipientUserId);
}
