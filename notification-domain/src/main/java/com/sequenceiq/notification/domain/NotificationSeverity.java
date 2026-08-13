package com.sequenceiq.notification.domain;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public enum NotificationSeverity {
    DEFAULT,
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL;

    public static NotificationSeverity fromString(String notificationSeverity) {
        if (StringUtils.isEmpty(notificationSeverity)) {
            return NotificationSeverity.ERROR;
        } else {
            try {
                return NotificationSeverity.valueOf(notificationSeverity.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NotificationSeverity.ERROR;
            }
        }
    }
}
