package com.sequenceiq.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.sender.CentralNotificationSenderService;
import com.sequenceiq.notification.sender.converter.NotificationToNotificationDtoConverter;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;
import com.sequenceiq.notification.sender.dto.NotificationSendingResult;

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
     * Sends an existing notification to the downstream processing system and returns both notifications and subscriptions
     */
    public NotificationSendingResult sendNotifications(List<Notification> notificationsForAccountAndType) {
        List<NotificationDto> notifications = convertNotifications(notificationsForAccountAndType);
        List<DistributionList> distributionLists = centralNotificationSenderService.processDistributionList(new NotificationSendingDtos(notifications));

        List<NotificationDto> notificationSendingDtos = centralNotificationSenderService.sendNotificationToDeliverySystem(
                new NotificationSendingDtos(notifications)
        );
        return new NotificationSendingResult(notificationSendingDtos, distributionLists);
    }

    private List<NotificationDto> convertNotifications(List<Notification> notificationsForAccountAndType) {
        return notificationsForAccountAndType.stream()
                .map(notificationToNotificationDtoConverter::convert)
                .toList();
    }

}
