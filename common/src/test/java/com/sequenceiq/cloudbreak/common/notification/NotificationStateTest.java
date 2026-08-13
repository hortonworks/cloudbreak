package com.sequenceiq.cloudbreak.common.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NotificationStateTest {

    @Test
    void testFromStringWithFallback() {
        assertEquals(NotificationState.ENABLED, NotificationState.fromStringStateWithDisableIfNull("ENABLED"));
        assertEquals(NotificationState.ENABLED, NotificationState.fromStringStateWithDisableIfNull("enabled"));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStringStateWithDisableIfNull("DISABLED"));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStringStateWithDisableIfNull("disabled"));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStringStateWithDisableIfNull("invalid"));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStringStateWithDisableIfNull(null));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStringStateWithDisableIfNull(""));
    }

    @Test
    void testFromStateWithFallback() {
        // notificationSendingEnabled = true: returns the state as-is, or ENABLED if null
        assertEquals(NotificationState.ENABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.ENABLED, true));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.DISABLED, true));
        assertEquals(NotificationState.ENABLED, NotificationState.fromStateWithDisableIfNull(null, true));

        // notificationSendingEnabled = false: always returns DISABLED regardless of input state
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.ENABLED, false));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.DISABLED, false));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(null, false));
    }
}
