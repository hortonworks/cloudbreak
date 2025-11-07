package com.sequenceiq.notification.sender.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.SeverityType;
import com.sequenceiq.notification.domain.NotificationSeverity;

@Component
public class NotificationSeverityConverter {

    public SeverityType.Value convert(NotificationSeverity severity) {
        if (severity == null) {
            return SeverityType.Value.DEFAULT;
        }

        return switch (severity) {
            case DEBUG -> SeverityType.Value.DEBUG;
            case INFO -> SeverityType.Value.INFO;
            case WARNING -> SeverityType.Value.WARNING;
            case ERROR -> SeverityType.Value.ERROR;
            case CRITICAL -> SeverityType.Value.CRITICAL;
        };
    }

    public NotificationSeverity convert(SeverityType.Value severityType) {
        if (severityType == null) {
            return NotificationSeverity.INFO;
        }

        return switch (severityType) {
            case DEBUG -> NotificationSeverity.DEBUG;
            case WARNING -> NotificationSeverity.WARNING;
            case ERROR -> NotificationSeverity.ERROR;
            case CRITICAL -> NotificationSeverity.CRITICAL;
            default -> NotificationSeverity.INFO;
        };
    }

    public NotificationSeverity safeSeverity(String value) {
        if (value == null) {
            return null;
        } else {
            try {
                return NotificationSeverity.valueOf(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
