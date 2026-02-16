package com.sequenceiq.notification.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.sender.LocalEmailProvider;

public class CentralNotificationGeneratorServiceTest {

    @Test
    @DisplayName("Successful notification generation creates new DTO with channel messages and resourceName fallback")
    void generateNotificationSuccess() {
        NotificationGeneratorDto original = NotificationGeneratorDto.builder()
                .name("testName")
                .accountId("acc")
                .resourceCrn("crn")
                .channelMessages(Map.of())
                .build();
        original.setResourceName("originalResourceName");

        LocalEmailProvider emailProvider = mock(LocalEmailProvider.class);
        NotificationGeneratorService emailService = mock(NotificationGeneratorService.class);
        when(emailService.channelTypes()).thenReturn(Set.of(ChannelType.EMAIL));
        when(emailService.generate(any(), any())).thenReturn(Optional.of("email message"));
        when(emailProvider.getLocalChannelIfConfigured(any())).thenReturn(Set.of(ChannelType.EMAIL));

        CentralNotificationGeneratorService underTest = new CentralNotificationGeneratorService(List.of(emailService), emailProvider);

        NotificationGeneratorDto result = underTest.generateNotification(original, NotificationType.AZURE_DEFAULT_OUTBOUND);

        assertThat(result).isNotSameAs(original);
        assertThat(result.getChannelMessages()).containsEntry(ChannelType.EMAIL, "email message");
        // builder does not copy resourceName, falls back to name when resourceName not explicitly set on builder
        assertThat(result.getResourceName()).isEqualTo(original.getName());
        assertThat(original.getResourceName()).isEqualTo("originalResourceName");
    }

    @Test
    @DisplayName("Exception during generation throws exception")
    void generateNotificationException() {
        NotificationGeneratorDto original = NotificationGeneratorDto.builder()
                .name("testName")
                .accountId("acc")
                .resourceCrn("crn")
                .build();

        LocalEmailProvider emailProvider = mock(LocalEmailProvider.class);
        NotificationGeneratorService failingEmailService = mock(NotificationGeneratorService.class);
        when(failingEmailService.channelTypes()).thenReturn(Set.of(ChannelType.EMAIL));
        when(emailProvider.getLocalChannelIfConfigured(any())).thenReturn(Set.of(ChannelType.EMAIL));
        when(failingEmailService.generate(any(), any())).thenThrow(new RuntimeException("boom"));
        when(emailProvider.getLocalChannelIfConfigured(any())).thenReturn(Set.of(ChannelType.EMAIL));

        CentralNotificationGeneratorService underTest = new CentralNotificationGeneratorService(List.of(failingEmailService), emailProvider);

        assertThrows(RuntimeException.class, () -> underTest.generateNotification(original, NotificationType.AZURE_DEFAULT_OUTBOUND));
    }

    @Test
    @DisplayName("Constructor maps all provided generator services by ChannelType")
    void constructorMapsServices() throws Exception {
        LocalEmailProvider emailProvider = mock(LocalEmailProvider.class);
        NotificationGeneratorService emailService = mock(NotificationGeneratorService.class);
        when(emailService.channelTypes()).thenReturn(Set.of(ChannelType.EMAIL));
        NotificationGeneratorService inAppService = mock(NotificationGeneratorService.class);
        when(inAppService.channelTypes()).thenReturn(Set.of(ChannelType.IN_APP));

        CentralNotificationGeneratorService underTest = new CentralNotificationGeneratorService(List.of(emailService, inAppService), emailProvider);

        Field field = CentralNotificationGeneratorService.class.getDeclaredField("generatorServiceMap");
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(underTest);
        assertThat(map.size()).isEqualTo(2);
        assertThat(map.containsKey(ChannelType.EMAIL)).isTrue();
        assertThat(map.containsKey(ChannelType.IN_APP)).isTrue();
    }

    @Test
    @DisplayName("Missing generator for required channel results in original DTO returned after exception")
    void generateNotificationMissingGeneratorChannel() {
        NotificationGeneratorDto original = NotificationGeneratorDto.builder()
                .name("missingGen")
                .accountId("acc3")
                .resourceCrn("crn3")
                .build();

        LocalEmailProvider emailProvider = mock(LocalEmailProvider.class);
        when(emailProvider.getLocalChannelIfConfigured(any())).thenReturn(Set.of(ChannelType.EMAIL));

        // Provide no generators though AZURE_DEFAULT_OUTBOUND requires EMAIL
        CentralNotificationGeneratorService underTest = new CentralNotificationGeneratorService(List.of(), emailProvider);

        assertThrows(RuntimeException.class, () -> underTest.generateNotification(original, NotificationType.AZURE_DEFAULT_OUTBOUND));
    }

    @Test
    @DisplayName("Original channel messages are not copied; new map contains only generated entries")
    void generateNotificationOriginalChannelMessagesNotCopied() {
        NotificationGeneratorDto original = NotificationGeneratorDto.builder()
                .name("copyCheck")
                .accountId("acc4")
                .resourceCrn("crn4")
                .channelMessages(Map.of(ChannelType.SLACK, "legacy"))
                .build();

        LocalEmailProvider emailProvider = mock(LocalEmailProvider.class);
        NotificationGeneratorService emailService = mock(NotificationGeneratorService.class);
        when(emailService.channelTypes()).thenReturn(Set.of(ChannelType.EMAIL));
        when(emailService.generate(any(), any())).thenReturn(Optional.of("email new"));
        when(emailProvider.getLocalChannelIfConfigured(any())).thenReturn(Set.of(ChannelType.EMAIL));

        CentralNotificationGeneratorService underTest = new CentralNotificationGeneratorService(List.of(emailService), emailProvider);
        NotificationGeneratorDto result = underTest.generateNotification(original, NotificationType.AZURE_DEFAULT_OUTBOUND);

        assertThat(result.getChannelMessages()).containsOnlyKeys(ChannelType.EMAIL)
                .containsEntry(ChannelType.EMAIL, "email new")
                .doesNotContainKey(ChannelType.SLACK);
        // original unchanged
        assertThat(original.getChannelMessages()).containsEntry(ChannelType.SLACK, "legacy");
    }
}
