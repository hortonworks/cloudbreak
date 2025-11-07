package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;

public class NotificationDtosToCreateDistributionListRequestConverterTest {

    private final NotificationDtosToCreateDistributionListRequestConverter underTest = new NotificationDtosToCreateDistributionListRequestConverter();

    @Test
    @DisplayName("convert null input returns empty set")
    void convertNullInputReturnsEmpty() {
        assertTrue(underTest.convert(null).isEmpty());
    }

    @Test
    @DisplayName("convert empty notifications list returns empty set")
    void convertEmptyNotificationsReturnsEmpty() {
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of());
        assertTrue(underTest.convert(dtos).isEmpty());
    }

    @Test
    @DisplayName("convert filters out notifications without distribution list form factor or null resourceCrn")
    void convertFiltersNonDistributionListAndNullCrn() {
        NotificationDto nonDl = NotificationDto.builder()
                .resourceCrn("crn:keep")
                .resourceName("keep")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.SUBSCRIPTION)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .severity(NotificationSeverity.INFO)
                .build();
        NotificationDto nullCrn = NotificationDto.builder()
                .resourceName("nullcrn")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .severity(NotificationSeverity.INFO)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(nonDl, nullCrn));
        Set<CreateDistributionListRequest> result = underTest.convert(dtos);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("convert aggregates preferences per resourceCrn combining channel types and severities")
    void convertAggregatesPerResourceCrn() {
        NotificationDto n1 = NotificationDto.builder()
                .resourceCrn("crn:cluster:1")
                .resourceName("cluster1")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .severity(NotificationSeverity.WARNING)
                .build();
        NotificationDto n2 = NotificationDto.builder()
                .resourceCrn("crn:cluster:1")
                .resourceName("cluster1-ignored")
                .channelType(ChannelType.SLACK)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .severity(NotificationSeverity.INFO)
                .build();
        NotificationDto n3 = NotificationDto.builder()
                .resourceCrn("crn:cluster:2")
                .resourceName("cluster2")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(n1, n2, n3));

        Set<CreateDistributionListRequest> result = underTest.convert(dtos);
        assertEquals(2, result.size());
        CreateDistributionListRequest c1 = result.stream().filter(r -> r.getResourceCrn().equals("crn:cluster:1")).findFirst().orElse(null);
        CreateDistributionListRequest c2 = result.stream().filter(r -> r.getResourceCrn().equals("crn:cluster:2")).findFirst().orElse(null);
        assertNotNull(c1);
        assertNotNull(c2);
        assertEquals("cluster1", c1.getResourceName());
        assertEquals("cluster2", c2.getResourceName());
        assertEquals(1, c1.getEventChannelPreferences().size());
        EventChannelPreference p1 = c1.getEventChannelPreferences().getFirst();
        assertEquals(Set.of(ChannelType.EMAIL, ChannelType.SLACK), p1.channelType());
        assertEquals(Set.of(NotificationSeverity.WARNING, NotificationSeverity.INFO), p1.eventSeverityList());
        EventChannelPreference p2 = c2.getEventChannelPreferences().getFirst();
        assertEquals(Set.of(ChannelType.EMAIL), p2.channelType());
        assertEquals(Set.of(NotificationSeverity.WARNING), p2.eventSeverityList());
    }

    @Test
    @DisplayName("convert ignores notifications with null type when aggregating")
    void convertIgnoresNullTypeNotifications() {
        NotificationDto valid = NotificationDto.builder()
                .resourceCrn("crn:valid")
                .resourceName("valid-name")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .severity(NotificationSeverity.INFO)
                .build();
        NotificationDto nullType = NotificationDto.builder()
                .resourceCrn("crn:valid")
                .resourceName("ignored")
                .channelType(ChannelType.SLACK)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .build();
        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(valid, nullType));
        Set<CreateDistributionListRequest> result = underTest.convert(dtos);
        assertEquals(1, result.size());
        CreateDistributionListRequest req = result.iterator().next();
        assertEquals("crn:valid", req.getResourceCrn());
        assertEquals("valid-name", req.getResourceName());
        assertEquals(1, req.getEventChannelPreferences().size());
        EventChannelPreference pref = req.getEventChannelPreferences().getFirst();
        assertEquals(Set.of(ChannelType.EMAIL), pref.channelType());
        assertEquals(Set.of(NotificationSeverity.INFO), pref.eventSeverityList());
    }

    @Test
    @DisplayName("convert aggregates preferences by different event types within same resource")
    void convertAggregatesByDifferentEventTypes() {
        // Create mock NotificationType instances with different eventTypeIds
        NotificationType type1 = createMockNotificationType("event1", NotificationSeverity.WARNING);
        NotificationType type2 = createMockNotificationType("event2", NotificationSeverity.INFO);

        NotificationDto n1 = NotificationDto.builder()
                .resourceCrn("crn:cluster:1")
                .resourceName("cluster1")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(type1)
                .severity(NotificationSeverity.CRITICAL)
                .build();

        // Second event type
        NotificationDto n2 = NotificationDto.builder()
                .resourceCrn("crn:cluster:1")
                .resourceName("cluster1")
                .channelType(ChannelType.SLACK)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(type2)
                .severity(NotificationSeverity.INFO)
                .build();

        // Second event type with different channel type
        NotificationDto n3 = NotificationDto.builder()
                .resourceCrn("crn:cluster:1")
                .resourceName("cluster1")
                .channelType(ChannelType.EMAIL)
                .notificationFormFactor(NotificationFormFactor.DISTRIBUTION_LIST)
                .type(type2)
                .severity(NotificationSeverity.ERROR)
                .build();

        NotificationSendingDtos dtos = new NotificationSendingDtos(List.of(n1, n2, n3));

        Set<CreateDistributionListRequest> result = underTest.convert(dtos);

        // Validate results
        assertEquals(1, result.size());
        CreateDistributionListRequest request = result.iterator().next();
        assertEquals("crn:cluster:1", request.getResourceCrn());
        assertEquals("cluster1", request.getResourceName());

        // Should have two separate event channel preferences
        assertEquals(2, request.getEventChannelPreferences().size());

        // Find preferences by event type ID
        EventChannelPreference pref1 = findPreferenceByEventType(request, "event1");
        EventChannelPreference pref2 = findPreferenceByEventType(request, "event2");

        assertNotNull(pref1, "Preference for event1 should exist");
        assertNotNull(pref2, "Preference for event2 should exist");

        // Verify first preference
        assertEquals(Set.of(ChannelType.EMAIL), pref1.channelType());
        assertEquals(Set.of(NotificationSeverity.CRITICAL), pref1.eventSeverityList());

        // Verify second preference
        assertEquals(Set.of(ChannelType.SLACK, ChannelType.EMAIL), pref2.channelType());
        assertEquals(Set.of(NotificationSeverity.INFO, NotificationSeverity.ERROR), pref2.eventSeverityList());
    }

    // Helper method to create mock NotificationType
    private NotificationType createMockNotificationType(String eventTypeId, NotificationSeverity defaultSeverity) {
        NotificationType mockType = mock(NotificationType.class);
        when(mockType.getEventTypeId()).thenReturn(eventTypeId);
        when(mockType.getNotificationSeverity()).thenReturn(defaultSeverity);
        // Mock other methods as needed
        when(mockType.getChannelTypes()).thenReturn(Set.of(ChannelType.EMAIL));
        when(mockType.getNotificationFormFactor()).thenReturn(NotificationFormFactor.DISTRIBUTION_LIST);
        return mockType;
    }

    // Helper method to find preference by event type
    private EventChannelPreference findPreferenceByEventType(CreateDistributionListRequest request, String eventTypeId) {
        return request.getEventChannelPreferences().stream()
                .filter(p -> p.eventType().equals(eventTypeId))
                .findFirst()
                .orElse(null);
    }

}
