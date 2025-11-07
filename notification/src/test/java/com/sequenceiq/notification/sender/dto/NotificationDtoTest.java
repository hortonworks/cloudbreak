package com.sequenceiq.notification.sender.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;

class NotificationDtoTest {

    @Test
    void validNotificationReturnsTrue() {
        NotificationDto dto = new NotificationDto();
        dto.setFormFactor(NotificationFormFactor.DISTRIBUTION_LIST);
        dto.setResourceCrn("test-crn");

        assertTrue(dto.isValidNotification(NotificationFormFactor.DISTRIBUTION_LIST));
    }

    @Test
    void invalidNotificationReturnsFalseWhenFormFactorIsNull() {
        NotificationDto dto = new NotificationDto();
        dto.setResourceCrn("test-crn");

        assertFalse(dto.isValidNotification(NotificationFormFactor.DISTRIBUTION_LIST));
    }

    @Test
    void invalidNotificationReturnsFalseWhenResourceCrnIsNull() {
        NotificationDto dto = new NotificationDto();
        dto.setFormFactor(NotificationFormFactor.DISTRIBUTION_LIST);

        assertFalse(dto.isValidNotification(NotificationFormFactor.DISTRIBUTION_LIST));
    }

    @Test
    void invalidNotificationReturnsFalseWhenFormFactorDoesNotMatch() {
        NotificationDto dto = new NotificationDto();
        dto.setFormFactor(NotificationFormFactor.SUBSCRIPTION);
        dto.setResourceCrn("test-crn");

        assertFalse(dto.isValidNotification(NotificationFormFactor.DISTRIBUTION_LIST));
    }

    @Test
    void builderCreatesNotificationWithAllFields() {
        NotificationDto dto = NotificationDto.builder()
                .severity(NotificationSeverity.ERROR)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .resourceCrn("test-crn")
                .resourceName("test-resource")
                .message("test-message")
                .created(System.currentTimeMillis())
                .sent(true)
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .metadata("test-metadata")
                .name("test-name")
                .accountId("test-account-id")
                .build();

        assertEquals(NotificationSeverity.ERROR, dto.getSeverity());
        assertEquals(NotificationType.AZURE_DEFAULT_OUTBOUND, dto.getType());
        assertEquals("test-crn", dto.getResourceCrn());
        assertEquals("test-resource", dto.getResourceName());
        assertEquals("test-message", dto.getMessage());
        assertTrue(dto.isSent());
        assertEquals(ChannelType.EMAIL, dto.getChannelType());
        assertEquals(NotificationFormFactor.DISTRIBUTION_LIST, dto.getFormFactor());
        assertEquals("test-metadata", dto.getMetadata());
        assertEquals("test-name", dto.getName());
        assertEquals("test-account-id", dto.getAccountId());
    }

}