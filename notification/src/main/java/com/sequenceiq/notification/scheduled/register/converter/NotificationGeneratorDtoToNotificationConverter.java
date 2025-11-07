package com.sequenceiq.notification.scheduled.register.converter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;

@Component
public class NotificationGeneratorDtoToNotificationConverter<T extends BaseNotificationRegisterAdditionalDataDtos> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationGeneratorDtoToNotificationConverter.class);

    public List<Notification> convert(NotificationGeneratorDto<T> notificationGeneratorDto, NotificationType notificationType) {
        if (notificationGeneratorDto.getChannelMessages() == null || notificationGeneratorDto.getChannelMessages().isEmpty()) {
            LOGGER.debug("No channel messages found for notification generator dto: {}", notificationGeneratorDto);
            return List.of();
        } else {
            List<Notification> notificationList = notificationGeneratorDto.getChannelMessages()
                    .entrySet()
                    .stream()
                    .map(entry ->
                            Notification.builder()
                                    .resourceCrn(notificationGeneratorDto.getResourceCrn())
                                    .resourceName(notificationGeneratorDto.getResourceName())
                                    .name(notificationGeneratorDto.getName())
                                    .type(notificationType)
                                    .severity(notificationType.getNotificationSeverity())
                                    .accountId(notificationGeneratorDto.getAccountId())
                                    .created(System.currentTimeMillis())
                                    .channelType(entry.getKey())
                                    .message(entry.getValue())
                                    .sent(false)
                                    .notificationFormFactor(notificationType.getNotificationFormFactor())
                                    .build()
                    )
                    .toList();
            LOGGER.debug("Generated notifications: {}", notificationList.size());
            return notificationList;
        }
    }
}
