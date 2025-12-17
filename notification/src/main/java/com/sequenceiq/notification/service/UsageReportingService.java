package com.sequenceiq.notification.service;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.dto.NotificationDto;

@Service
public class UsageReportingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageReportingService.class);

    @Inject
    private UsageReporter usageReporter;

    public void sendNotificationEvent(NotificationDto notificationDto) {
        UsageProto.CDPNotificationSentEvent.Builder builder = UsageProto.CDPNotificationSentEvent.newBuilder();
        setIfNotNull(builder::setAccountId, notificationDto.getAccountId());
        setIfNotNull(builder::setMessage, notificationDto.getMessage());
        setIfNotNull(builder::setName, notificationDto.getName());
        setIfNotNull(builder::setResourceCrn, notificationDto.getResourceCrn());
        setIfNotNull(builder::setResourceName, notificationDto.getResourceName());
        if (notificationDto.getCreatedAt() != null) {
            builder.setCreatedAt(notificationDto.getCreatedAt());
        }
        if (notificationDto.getSentAt() != null) {
            builder.setSentAt(notificationDto.getSentAt());
        }
        builder.setSent(notificationDto.isSent());
        if (notificationDto.getSeverity() != null) {
            builder.setNotificationSeverity(getNotificationSeverity(notificationDto.getSeverity()));
        }
        if (notificationDto.getFormFactor() != null) {
            builder.setNotificationFormFactor(getNotificationFormFactor(notificationDto.getFormFactor()));
        }
        if (notificationDto.getType() != null) {
            builder.setNotificationType(getNotificationType(notificationDto.getType()));
        }
        if (notificationDto.getChannelType() != null) {
            builder.setChannelType(getChannelType(notificationDto.getChannelType()));
        }

        UsageProto.CDPNotificationSentEvent event = builder.build();
        LOGGER.debug("Reporting usage event: {}", notificationDto.getType());
        usageReporter.cdpNotificationSentEvent(event);
    }

    private <T> void setIfNotNull(Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private UsageProto.NotificationFormFactor.Value getNotificationFormFactor(NotificationFormFactor notificationFormFactor) {
        return switch (notificationFormFactor) {
            case SUBSCRIPTION -> UsageProto.NotificationFormFactor.Value.SUBSCRIPTION;
            case DISTRIBUTION_LIST -> UsageProto.NotificationFormFactor.Value.DISTRIBUTION_LIST;
            case SYSTEM_MANAGED_DISTRIBUTION_LIST -> UsageProto.NotificationFormFactor.Value.SYSTEM_MANAGED_DISTRIBUTION_LIST;
        };
    }

    private UsageProto.NotificationType.Value getNotificationType(NotificationType notificationType) {
        return switch (notificationType) {
            case AZURE_DEFAULT_OUTBOUND -> UsageProto.NotificationType.Value.AZURE_DEFAULT_OUTBOUND;
        };
    }

    private UsageProto.NotificationSeverity.Value getNotificationSeverity(NotificationSeverity notificationSeverity) {
        return switch (notificationSeverity) {
            case DEBUG -> UsageProto.NotificationSeverity.Value.DEBUG;
            case INFO -> UsageProto.NotificationSeverity.Value.INFO;
            case WARNING -> UsageProto.NotificationSeverity.Value.WARNING;
            case ERROR -> UsageProto.NotificationSeverity.Value.ERROR;
            case CRITICAL -> UsageProto.NotificationSeverity.Value.CRITICAL;
        };
    }

    private UsageProto.ChannelType.Value getChannelType(ChannelType channelType) {
        return switch (channelType) {
            case EMAIL -> UsageProto.ChannelType.Value.EMAIL;
            case SLACK -> UsageProto.ChannelType.Value.SLACK;
            case IN_APP -> UsageProto.ChannelType.Value.IN_APP;
        };
    }
}