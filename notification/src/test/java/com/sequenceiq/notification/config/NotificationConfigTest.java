package com.sequenceiq.notification.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@ExtendWith(MockitoExtension.class)
class NotificationConfigTest {
    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private NotificationConfig notificationConfig;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationConfig, "enabled", true);
    }

    @Test
    void testIsEnabledTrue() {
        Crn crn = Crn.fromString("crn:cdp:environments:us-west-1:cloudera:environment:4428e540-a878-42b1-a1d4-91747322d8b6");
        when(entitlementService.isCdpCbNotificationSendingEnabled("cloudera")).thenReturn(true);
        boolean result = notificationConfig.isEnabled(crn);
        assertTrue(result);
        verify(entitlementService).isCdpCbNotificationSendingEnabled("cloudera");
    }

    @Test
    void testIsEnabledFalseWhenDisabled() {
        ReflectionTestUtils.setField(notificationConfig, "enabled", false);
        Crn crn = Crn.fromString("crn:cdp:environments:us-west-1:cloudera:environment:4428e540-a878-42b1-a1d4-91747322d8b6");
        boolean result = notificationConfig.isEnabled(crn);
        assertFalse(result);
        verify(entitlementService, never()).isCdpCbNotificationSendingEnabled(anyString());
    }
}