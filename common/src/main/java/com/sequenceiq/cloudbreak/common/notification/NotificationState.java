package com.sequenceiq.cloudbreak.common.notification;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum NotificationState {
    ENABLED,
    DISABLED;

    public static NotificationState fromStringStateWithDisableIfNull(String notificationState) {
        return fromString(notificationState, true);
    }

    public static NotificationState fromStateWithDisableIfNull(NotificationState notificationState) {
        return notificationState == null ? NotificationState.DISABLED : notificationState;
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
