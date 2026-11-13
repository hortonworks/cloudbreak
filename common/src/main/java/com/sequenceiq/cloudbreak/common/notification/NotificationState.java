package com.sequenceiq.cloudbreak.common.notification;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum NotificationState {
    ENABLED,
    DISABLED;

    public static NotificationState fromStringStateWithDisableIfNull(String notificationState) {
        return fromString(notificationState, true);
    }

    /**
     * Resolves the notification state to persist for a new resource. Notifications are disabled by default: an
     * explicitly requested state is only honoured when notification sending is enabled for the account.
     */
    public static NotificationState fromStateWithDisableIfNull(NotificationState notificationState, boolean notificationSendingEnabled) {
        if (notificationSendingEnabled && notificationState != null) {
            return notificationState;
        }
        return DISABLED;
    }

    private static NotificationState fromString(String notificationState, boolean disableIfNull) {
        if (StringUtils.isEmpty(notificationState)) {
            return disableIfNull ? DISABLED : null;
        }
        if (Arrays.stream(values()).noneMatch(ns -> ns.name().equalsIgnoreCase(notificationState))) {
            return DISABLED;
        }
        return valueOf(notificationState.toUpperCase());
    }
}
