package com.sequenceiq.notification.sender.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.sender.dto.NotificationDto;

@Component
public class NotificationToNotificationDtoConverter {

    public NotificationDto convert(Notification notification) {
        return NotificationDto.builder()
                .notification(notification)
                .build();
    }
}
