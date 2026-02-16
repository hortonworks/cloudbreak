package com.sequenceiq.notification.scheduled.register.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;

class NotificationGeneratorDtoToNotificationStateonverterTest {

    private final NotificationGeneratorDtoToNotificationConverter converter
            = new NotificationGeneratorDtoToNotificationConverter();

    @Test
    void convertReturnsEmptyListWhenChannelMessagesIsEmpty() {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        dto.setChannelMessages(Map.of());
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;

        List<Notification> result = converter.convert(dto, type);

        assertThat(result).isEmpty();
    }

    @Test
    void convertCreatesNotificationsForEachChannelMessage() {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        dto.setResourceCrn("crn1");
        dto.setResourceName("resourceName");
        dto.setName("notificationName");
        dto.setAccountId("accountId");
        dto.setChannelMessages(Map.of(
                ChannelType.EMAIL, "Email message"
        ));
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;

        List<Notification> result = converter.convert(dto, type);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(Notification::getChannelType)
                .containsExactlyInAnyOrder(ChannelType.EMAIL);
    }

    @Test
    void convertSetsCorrectNotificationProperties() {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        dto.setResourceCrn("crn1");
        dto.setResourceName("resourceName");
        dto.setName("notificationName");
        dto.setAccountId("accountId");
        dto.setChannelMessages(Map.of(ChannelType.EMAIL, "Email message"));
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;

        List<Notification> result = converter.convert(dto, type);

        Notification notification = result.get(0);
        assertThat(notification.getResourceCrn()).isEqualTo("crn1");
        assertThat(notification.getResourceName()).isEqualTo("resourceName");
        assertThat(notification.getName()).isEqualTo("notificationName");
        assertThat(notification.getType()).isEqualTo(type);
        assertThat(notification.getMessage()).isEqualTo("Email message");
        assertThat(notification.isSent()).isFalse();
    }

    @Test
    void convertHandlesNullChannelMessagesGracefully() {
        NotificationGeneratorDto dto = new NotificationGeneratorDto();
        dto.setChannelMessages(Map.of());
        NotificationType type = NotificationType.AZURE_DEFAULT_OUTBOUND;

        List<Notification> result = converter.convert(dto, type);

        assertThat(result).isEmpty();
    }
}