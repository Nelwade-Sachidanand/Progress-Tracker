package com.dashboard.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novillex.progresstracker.entity.Notification;
import com.novillex.progresstracker.repository.NotificationRepository;
import com.novillex.progresstracker.serviceImpl.NotificationServiceImpl;
import com.novillex.progresstracker.util.UserContextUtil;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	@Mock
	private NotificationRepository notificationRepository;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private MockedStatic<UserContextUtil> userContextMock;

	@BeforeEach
	void setup() {

		userContextMock = mockStatic(UserContextUtil.class);

		userContextMock.when(UserContextUtil::getCurrentUser).thenReturn("testUser");
	}

	@AfterEach
	void tearDown() {

		userContextMock.close();
	}

	@Test
	void createNotification_Success() {

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		notificationService.createNotification("Activity Updated", "Activity updated successfully", "ACTIVITY",
				"REF001", "/dashboard", "USER001");

		verify(notificationRepository, times(1)).save(captor.capture());

		Notification notification = captor.getValue();

		assertAll(

				() -> assertEquals("Activity Updated", notification.getTitle()),
				() -> assertEquals("Activity updated successfully", notification.getMessage()),
				() -> assertEquals("ACTIVITY", notification.getType()),
				() -> assertEquals("REF001", notification.getReferenceId()),
				() -> assertEquals("/dashboard", notification.getRedirectUrl()),
				() -> assertEquals("USER001", notification.getRecipientUserId()),
				() -> assertEquals("testUser", notification.getCreatedBy()),
				() -> assertFalse(notification.isRead()),
				() -> assertNotNull(notification.getCreatedAt()));

		verifyNoMoreInteractions(notificationRepository);
	}

	@Test
	void createNotification_RepositoryThrowsException() {

		doThrow(new RuntimeException("Database Error")).when(notificationRepository).save(any(Notification.class));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> notificationService
				.createNotification("Title", "Message", "TYPE", "REF001", "/dashboard", "USER001"));

		assertEquals("Database Error", exception.getMessage());

		verify(notificationRepository).save(any(Notification.class));
	}

	@Test
	void createNotification_ShouldUseCurrentLoggedInUser() {

		userContextMock.when(UserContextUtil::getCurrentUser).thenReturn("admin");

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		notificationService.createNotification("Title", "Message", "TYPE", "REF001", "/dashboard", "USER001");

		verify(notificationRepository).save(captor.capture());

		assertEquals("admin", captor.getValue().getCreatedBy());
	}

	@Test
	void createNotification_ShouldCreateIndependentNotificationObjects() {

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

		notificationService.createNotification("Title1", "Message1", "TYPE1", "REF001", "/dashboard1", "USER001");

		notificationService.createNotification("Title2", "Message2", "TYPE2", "REF002", "/dashboard2", "USER002");

		verify(notificationRepository, times(2)).save(captor.capture());

		assertEquals(2, captor.getAllValues().size());

		Notification first = captor.getAllValues().get(0);
		Notification second = captor.getAllValues().get(1);

		assertNotSame(first, second);

		assertAll(
				() -> assertEquals("Title1", first.getTitle()),
				() -> assertEquals("Message1", first.getMessage()),
				() -> assertEquals("USER001", first.getRecipientUserId()),
				() -> assertEquals("Title2", second.getTitle()),
				() -> assertEquals("Message2", second.getMessage()),
				() -> assertEquals("USER002", second.getRecipientUserId()));
	}
}