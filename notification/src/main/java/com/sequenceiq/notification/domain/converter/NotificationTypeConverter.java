package com.sequenceiq.notification.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.notification.domain.NotificationType;

public class NotificationTypeConverter extends DefaultEnumConverter<NotificationType> {

    @Override
    public NotificationType getDefault() {
        return NotificationType.AZURE_DEFAULT_OUTBOUND;
    }
}
