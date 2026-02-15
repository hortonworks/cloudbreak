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
        assertEquals(NotificationState.ENABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.ENABLED));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(NotificationState.DISABLED));
        assertEquals(NotificationState.DISABLED, NotificationState.fromStateWithDisableIfNull(null));
    }
}
