package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.novillex.progresstracker.entity.Notification;
import com.novillex.progresstracker.repository.NotificationRepository;
import com.novillex.progresstracker.serviceImpl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	@BeforeEach
	void setup() {

		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testUser", null));
	}

	@Test
	void shouldCreateNotificationSuccessfully() {

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		notificationService.createNotification("Activity Updated", "Activity updated successfully", "ACTIVITY",
				"REF001", "/dashboard");

		verify(notificationRepository).save(captor.capture());

		Notification notification = captor.getValue();

		assertAll(

				() -> assertEquals("Activity Updated", notification.getTitle()),

				() -> assertEquals("Activity updated successfully", notification.getMessage()),

				() -> assertEquals("ACTIVITY", notification.getType()),

				() -> assertEquals("REF001", notification.getReferenceId()),

				() -> assertEquals("/dashboard", notification.getRedirectUrl()),

				() -> assertEquals("testUser", notification.getCreatedBy()),

				() -> assertFalse(notification.isRead()),

				() -> assertNotNull(notification.getCreatedAt()));
	}

	@Test
	void shouldThrowExceptionWhenRepositorySaveFails() {

		doThrow(new RuntimeException("Database Error")).when(notificationRepository).save(any(Notification.class));

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> notificationService.createNotification("Title", "Message", "TYPE", "REF001", "/dashboard"));

		assertEquals("Database Error", ex.getMessage());
	}

	@Test
	void shouldSaveNotificationOnlyOnce() {

		notificationService.createNotification("Title", "Message", "TYPE", "REF001", "/dashboard");

		verify(notificationRepository, times(1)).save(any(Notification.class));
	}
}