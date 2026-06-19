package com.novillex.progresstracker.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.novillex.progresstracker.entity.Notification;
import com.novillex.progresstracker.repository.NotificationRepository;
import com.novillex.progresstracker.service.NotificationService;
import com.novillex.progresstracker.util.UserContextUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;

	@Override
	public void createNotification(String title, String message, String type, String referenceId, String redirectUrl) {

		Notification notification = new Notification();

		notification.setTitle(title);
		notification.setMessage(message);
		notification.setType(type);
		notification.setReferenceId(referenceId);
		notification.setRedirectUrl(redirectUrl);
		notification.setCreatedBy(UserContextUtil.getCurrentUser());
		notification.setRead(false);
		notification.setCreatedAt(LocalDateTime.now());

		notificationRepository.save(notification);
	}
}
