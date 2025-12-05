package com.sequenceiq.cloudbreak.notification.client.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto;

class GetPublishedEventStatusResponseConverterTest {

    private static final String EVENT_ID = "event-123";

    private static final String EVENT_TYPE_ID = "event-type-456";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:account:cluster:resource-123";

    private static final String TITLE = "Test Event";

    private static final String DESCRIPTION = "Test description";

    private static final String TARGETED_EVENT_TYPE = "TARGETED";

    private static final long CREATED_AT = 1234567890L;

    private GetPublishedEventStatusResponseConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new GetPublishedEventStatusResponseConverter();
    }

    @Test
    void convertNullProtoReturnsEmptyDto() {
        GetPublishedEventStatusResponseDto result = underTest.convert(null);

        assertNotNull(result);
        assertNull(result.publishedEventId());
        assertNull(result.eventTypeId());
        assertNull(result.title());
        assertNull(result.resourceCrn());
        assertNull(result.targetedEventType());
        assertNull(result.description());
        assertNotNull(result.channelStatuses());
        assertTrue(result.channelStatuses().isEmpty());
        assertEquals(0L, result.createdAt());
    }

    @Test
    void convertValidProtoMapsAllFields() {
        NotificationAdminProto.GetPublishedEventStatusResponse proto = createProto(EVENT_ID, EVENT_TYPE_ID, TITLE,
                RESOURCE_CRN, TARGETED_EVENT_TYPE, DESCRIPTION, CREATED_AT);

        GetPublishedEventStatusResponseDto result = underTest.convert(proto);

        assertNotNull(result);
        assertEquals(EVENT_ID, result.publishedEventId());
        assertEquals(EVENT_TYPE_ID, result.eventTypeId());
        assertEquals(TITLE, result.title());
        assertEquals(RESOURCE_CRN, result.resourceCrn());
        assertEquals(TARGETED_EVENT_TYPE, result.targetedEventType());
        assertEquals(DESCRIPTION, result.description());
        assertEquals(CREATED_AT, result.createdAt());
    }

    @Test
    void convertProtoWithEmptyChannelStatusesReturnsEmptyList() {
        NotificationAdminProto.GetPublishedEventStatusResponse proto = createProto(EVENT_ID, EVENT_TYPE_ID, TITLE,
                RESOURCE_CRN, TARGETED_EVENT_TYPE, DESCRIPTION, CREATED_AT);

        GetPublishedEventStatusResponseDto result = underTest.convert(proto);

        assertNotNull(result.channelStatuses());
        assertTrue(result.channelStatuses().isEmpty());
    }

    @Test
    void convertProtoWithMultipleChannelStatusesReturnsAllInOrder() {
        NotificationAdminProto.GetPublishedEventStatusResponse proto = createProtoWithChannelStatuses(
                NotificationAdminProto.ChannelType.Value.EMAIL, NotificationAdminProto.EventStatus.Value.PROCESSED,
                NotificationAdminProto.ChannelType.Value.IN_APP, NotificationAdminProto.EventStatus.Value.UNPROCESSED,
                NotificationAdminProto.ChannelType.Value.SLACK, NotificationAdminProto.EventStatus.Value.PROCESSED);

        GetPublishedEventStatusResponseDto result = underTest.convert(proto);

        assertNotNull(result.channelStatuses());
        assertEquals(3, result.channelStatuses().size());
        assertEquals("EMAIL", result.channelStatuses().get(0).channel());
        assertEquals("PROCESSED", result.channelStatuses().get(0).status());
        assertEquals("IN_APP", result.channelStatuses().get(1).channel());
        assertEquals("UNPROCESSED", result.channelStatuses().get(1).status());
        assertEquals("SLACK", result.channelStatuses().get(2).channel());
        assertEquals("PROCESSED", result.channelStatuses().get(2).status());
    }

    @ParameterizedTest
    @MethodSource("provideChannelTypes")
    void convertHandlesAllChannelTypes(NotificationAdminProto.ChannelType.Value channelType) {
        NotificationAdminProto.GetPublishedEventStatusResponse proto = createProtoWithChannelStatuses(
                channelType, NotificationAdminProto.EventStatus.Value.PROCESSED);

        GetPublishedEventStatusResponseDto result = underTest.convert(proto);

        assertEquals(channelType.name(), result.channelStatuses().get(0).channel());
    }

    @ParameterizedTest
    @MethodSource("provideEventStatusTypes")
    void convertHandlesAllEventStatusTypes(NotificationAdminProto.EventStatus.Value eventStatus) {
        NotificationAdminProto.GetPublishedEventStatusResponse proto = createProtoWithChannelStatuses(
                NotificationAdminProto.ChannelType.Value.EMAIL, eventStatus);

        GetPublishedEventStatusResponseDto result = underTest.convert(proto);

        assertEquals(eventStatus.name(), result.channelStatuses().get(0).status());
    }

    private static Stream<Arguments> provideChannelTypes() {
        return Stream.of(
                Arguments.of(NotificationAdminProto.ChannelType.Value.EMAIL),
                Arguments.of(NotificationAdminProto.ChannelType.Value.IN_APP),
                Arguments.of(NotificationAdminProto.ChannelType.Value.SLACK)
        );
    }

    private static Stream<Arguments> provideEventStatusTypes() {
        return Stream.of(
                Arguments.of(NotificationAdminProto.EventStatus.Value.UNPROCESSED),
                Arguments.of(NotificationAdminProto.EventStatus.Value.PROCESSED)
        );
    }

    private NotificationAdminProto.GetPublishedEventStatusResponse createProto(String eventId, String eventTypeId,
            String title, String resourceCrn, String targetedEventType, String description, long createdAt) {
        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId(eventId)
                .setEventTypeId(eventTypeId)
                .setTitle(title)
                .setResourceCrn(resourceCrn)
                .setTargetedEventType(targetedEventType)
                .setDescription(description)
                .setCreatedAt(createdAt)
                .build();

        return NotificationAdminProto.GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatus)
                .build();
    }

    private NotificationAdminProto.GetPublishedEventStatusResponse createProtoWithChannelStatuses(Object... channelStatusPairs) {
        NotificationAdminProto.PublishedEventStatus.Builder eventStatusBuilder = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId(EVENT_ID)
                .setEventTypeId(EVENT_TYPE_ID)
                .setTitle(TITLE)
                .setResourceCrn(RESOURCE_CRN)
                .setTargetedEventType(TARGETED_EVENT_TYPE)
                .setDescription(DESCRIPTION)
                .setCreatedAt(CREATED_AT);

        for (int i = 0; i < channelStatusPairs.length; i += 2) {
            NotificationAdminProto.ChannelType.Value channelType = (NotificationAdminProto.ChannelType.Value) channelStatusPairs[i];
            NotificationAdminProto.EventStatus.Value eventStatus = (NotificationAdminProto.EventStatus.Value) channelStatusPairs[i + 1];
            eventStatusBuilder.addStatus(NotificationAdminProto.ChannelEventStatus.newBuilder()
                    .setChannelType(channelType)
                    .setEventStatus(eventStatus)
                    .build());
        }

        return NotificationAdminProto.GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatusBuilder.build())
                .build();
    }
}