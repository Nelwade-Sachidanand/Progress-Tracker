package com.novillex.progresstracker.resources;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.common.Response;
import com.novillex.progresstracker.common.ResponseBuilder;
import com.novillex.progresstracker.common.StatusCode;
import com.novillex.progresstracker.entity.Notification;
import com.novillex.progresstracker.exception.ResourceNotFoundException;
import com.novillex.progresstracker.repository.NotificationRepository;
import com.novillex.progresstracker.util.UserContextUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationRepository notificationRepository;

	
	private ApplicationContext context;
	
	public NotificationController(NotificationRepository notificationRepository, ApplicationContext context) {
		this.notificationRepository=notificationRepository;
		this.context=context;
	}

	@GetMapping
	public Response getNotifications() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		String userId = UserContextUtil.getCurrentUserId();

		String role = UserContextUtil.getCurrentUserRole();

		List<Notification> notifications;

		if ("ADMIN".equalsIgnoreCase(role)) {

			notifications = notificationRepository
					.findByRecipientUserIdIsNullAndReadFalse(Sort.by(Sort.Direction.DESC, "createdAt"));

		} else {

			notifications = notificationRepository.findByRecipientUserIdAndReadFalse(userId,
					Sort.by(Sort.Direction.DESC, "createdAt"));
		}

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Unread notifications fetched successfully", notifications);
	}

	@PutMapping("/{id}/read")
	public Response markAsRead(@PathVariable String id) {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		Notification notification = notificationRepository.findById(id).orElseThrow(
				() -> new ResourceNotFoundException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found", id));

		notification.setRead(true);

		notificationRepository.save(notification);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Notification marked as read", notification);
	}

	@PutMapping("/read-all")
	public Response markAllRead() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		List<Notification> notifications = notificationRepository.findAll();

		notifications.forEach(notification -> notification.setRead(true));

		notificationRepository.saveAll(notifications);

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"All notifications marked as read", notifications.size());
	}

	@GetMapping("/unread-count")
	public Response getUnreadCount() {

		ResponseBuilder responseBuilder = context.getBean(ResponseBuilder.class);

		String userId = UserContextUtil.getCurrentUserId();

		String role = UserContextUtil.getCurrentUserRole();
		
		System.out.println("userId = "+userId);
		System.out.println("role = "+role);

		long count = 0;

		if ("ADMIN".equalsIgnoreCase(role)) {

			count = notificationRepository.countByRecipientUserIdIsNullAndReadFalse();

		} else {

			count = notificationRepository.countByRecipientUserIdAndReadFalse(userId);
		}

		return responseBuilder.createResponse(StatusCode.SUCCESS, StatusCode.SUCCESS_STATUS_TYPE,
				"Unread count fetched successfully", count);
	}

}
