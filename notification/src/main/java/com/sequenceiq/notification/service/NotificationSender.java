package com.sequenceiq.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.sender.CentralNotificationSenderService;
import com.sequenceiq.notification.sender.converter.NotificationToNotificationDtoConverter;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;

@Service
public class NotificationSender {

    private final CentralNotificationSenderService centralNotificationSenderService;

    private final NotificationToNotificationDtoConverter notificationToNotificationDtoConverter;

    public NotificationSender(
            CentralNotificationSenderService centralNotificationSenderService,
            NotificationToNotificationDtoConverter notificationToNotificationDtoConverter) {
        this.centralNotificationSenderService = centralNotificationSenderService;
        this.notificationToNotificationDtoConverter = notificationToNotificationDtoConverter;
    }

    /**
     * Sends an existing notification to the downstream processing system
     */
    public List<NotificationDto> sendNotifications(List<Notification> notificationsForAccountAndType) {
        List<NotificationDto> notificationSendingDtos = centralNotificationSenderService.sendNotificationToDeliverySystem(
                new NotificationSendingDtos(convertNotifications(notificationsForAccountAndType))
        );
        return notificationSendingDtos;
    }

    private List<NotificationDto> convertNotifications(List<Notification> notificationsForAccountAndType) {
        return notificationsForAccountAndType.stream()
                .map(notificationToNotificationDtoConverter::convert)
                .toList();
    }

}
