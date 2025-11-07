package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.dto.NotificationDto;

public class NotificationToNotificationDtoConverterTest {

    private final NotificationToNotificationDtoConverter underTest = new NotificationToNotificationDtoConverter();

    @Test
    @DisplayName("convert should copy all fields from Notification to NotificationDto and leave id null")
    void testConvertCopiesFields() {
        long createdAt = 1720000000000L;
        long sentAt = createdAt + 1000L;
        Notification notification = Notification.builder()
                .severity(NotificationSeverity.WARNING)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .resourceCrn("crn:test:resource")
                .resourceName("resourceName")
                .name("notificationName")
                .message("Test message")
                .created(createdAt)
                .sentAt(sentAt)
                .sent(true)
                .accountId("accountId")
                .build();

        NotificationDto dto = underTest.convert(notification);

        assertEquals(notification.getSeverity(), dto.getSeverity());
        assertEquals(notification.getType(), dto.getType());
        assertEquals(notification.getChannelType(), dto.getChannelType());
        assertEquals(notification.getFormFactor(), dto.getFormFactor());
        assertEquals(notification.getResourceCrn(), dto.getResourceCrn());
        assertEquals(notification.getResourceName(), dto.getResourceName());
        assertEquals(notification.getName(), dto.getName());
        assertEquals(notification.getAccountId(), dto.getAccountId());
        assertEquals(notification.getMessage(), dto.getMessage());
        assertEquals(notification.getCreatedAt(), dto.getCreatedAt());
        assertEquals(notification.getSentAt(), dto.getSentAt());
        assertEquals(notification.isSent(), dto.isSent());
    }

    @Test
    @DisplayName("convert should fallback createdAt when source notification has null createdAt")
    void testConvertCreatedAtFallback() {
        Notification notification = new Notification();
        notification.setSeverity(NotificationSeverity.INFO);
        notification.setType(NotificationType.AZURE_DEFAULT_OUTBOUND);
        notification.setChannelType(ChannelType.EMAIL);
        notification.setFormFactor(NotificationFormFactor.DISTRIBUTION_LIST);
        // createdAt intentionally left null
        notification.setMessage("Fallback message");
        notification.setResourceCrn("crn:fallback:resource");
        notification.setResourceName("fallbackResource");
        notification.setName("fallbackName");
        notification.setAccountId("fallbackAccount");
        notification.setSent(false);

        long before = System.currentTimeMillis();
        NotificationDto dto = underTest.convert(notification);
        long after = System.currentTimeMillis();

        assertNotNull(dto.getCreatedAt(), "CreatedAt should be auto-populated when null in source");
        assertTrue(dto.getCreatedAt() >= before && dto.getCreatedAt() <= after, "CreatedAt should be within invocation window");
    }

    @Test
    @DisplayName("convert should throw NullPointerException when notification is null")
    void testConvertNullNotification() {
        assertThrows(NullPointerException.class, () -> underTest.convert(null));
    }
}
