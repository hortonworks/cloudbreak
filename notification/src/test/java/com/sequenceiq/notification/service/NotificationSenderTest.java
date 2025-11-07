package com.sequenceiq.notification.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.repository.NotificationDataAccessService;
import com.sequenceiq.notification.sender.CentralNotificationSenderService;
import com.sequenceiq.notification.sender.converter.NotificationToNotificationDtoConverter;
import com.sequenceiq.notification.sender.dto.NotificationDto;

class NotificationSenderTest {

    private NotificationDataAccessService notificationService;

    private CentralNotificationSenderService centralNotificationSenderService;

    private NotificationToNotificationDtoConverter notificationToNotificationDtoConverter;

    private NotificationSender notificationSender;

    @BeforeEach
    void setUp() {
        notificationService = org.mockito.Mockito.mock(NotificationDataAccessService.class);
        centralNotificationSenderService = org.mockito.Mockito.mock(CentralNotificationSenderService.class);
        notificationToNotificationDtoConverter = org.mockito.Mockito.mock(NotificationToNotificationDtoConverter.class);
        notificationSender = new NotificationSender(
                centralNotificationSenderService,
                notificationToNotificationDtoConverter
        );
    }

    @Test
    void sendNotificationsProcessesAllNotificationsSuccessfully() {
        Notification notification1 = Notification.builder()
                .name("Test Notification 1")
                .build();
        Notification notification2 = Notification.builder()
                .name("Test Notification 2")
                .build();
        List<Notification> notifications = List.of(notification1, notification2);
        NotificationDto dto1 = NotificationDto.builder()
                .name("Test Notification 1")
                .build();
        NotificationDto dto2 = NotificationDto.builder()
                .name("Test Notification 2")
                .build();

        when(notificationToNotificationDtoConverter.convert(notification1)).thenReturn(dto1);
        when(notificationToNotificationDtoConverter.convert(notification2)).thenReturn(dto2);
        when(centralNotificationSenderService.sendNotificationToDeliverySystem(any())).thenReturn(
                List.of(dto1, dto2)
        );

        notificationSender.sendNotifications(notifications);

        verify(centralNotificationSenderService).sendNotificationToDeliverySystem(any());
    }

    @Test
    void sendNotificationsHandlesEmptyNotificationListWithoutErrors() {
        List<Notification> notifications = List.of();

        notificationSender.sendNotifications(notifications);

        verify(centralNotificationSenderService).sendNotificationToDeliverySystem(any());
    }

    @Test
    void sendNotificationsThrowsExceptionWhenConversionFails() {
        Notification notification = new Notification();
        List<Notification> notifications = List.of(notification);

        when(notificationToNotificationDtoConverter.convert(notification)).thenThrow(new RuntimeException("Conversion failed"));

        assertThrows(RuntimeException.class, () -> notificationSender.sendNotifications(notifications));

        verifyNoInteractions(centralNotificationSenderService);
    }
}