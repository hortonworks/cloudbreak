package com.sequenceiq.notification.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.notification.domain.NotificationSeverity;

public class NotificationSeverityConverter extends DefaultEnumConverter<NotificationSeverity> {

    @Override
    public NotificationSeverity getDefault() {
        return NotificationSeverity.INFO;
    }
}
