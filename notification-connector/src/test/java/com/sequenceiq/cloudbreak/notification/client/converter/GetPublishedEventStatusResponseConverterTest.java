package com.sequenceiq.cloudbreak.notification.client.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto.ChannelStatusDto;

class GetPublishedEventStatusResponseConverterTest {

    private final GetPublishedEventStatusResponseConverter underTest = new GetPublishedEventStatusResponseConverter();

    @Test
    void testConvertNullProto() {
        GetPublishedEventStatusResponseDto dto = underTest.convert(null);
        assertNotNull(dto, "Should return a non-null DTO");
        assertNull(dto.publishedEventId());
        assertNull(dto.eventTypeId());
        assertNull(dto.title());
        assertNull(dto.resourceCrn());
        assertNull(dto.targetedEventType());
        assertNull(dto.description());
        assertTrue(dto.channelStatuses().isEmpty(), "Channel statuses should be null for default ctor");
        assertEquals(0L, dto.createdAt());
    }

    @Test
    void testConvertWithEmptyStatusList() {
        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId("pub-1")
                .setEventTypeId("event-type-1")
                .setTitle("Title One")
                .setResourceCrn("crn:cdp:env:us-west-1:tenant:environment:env1")
                .setTargetedEventType("TARGET")
                .setDescription("No channels yet")
                .setCreatedAt(12345L)
                .build();

        NotificationAdminProto.GetPublishedEventStatusResponse proto = NotificationAdminProto
                .GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatus)
                .build();

        GetPublishedEventStatusResponseDto dto = underTest.convert(proto);
        assertEquals("pub-1", dto.publishedEventId());
        assertEquals("event-type-1", dto.eventTypeId());
        assertEquals("Title One", dto.title());
        assertEquals("crn:cdp:env:us-west-1:tenant:environment:env1", dto.resourceCrn());
        assertEquals("TARGET", dto.targetedEventType());
        assertEquals("No channels yet", dto.description());
        assertEquals(12345L, dto.createdAt());
        assertNotNull(dto.channelStatuses());
        assertTrue(dto.channelStatuses().isEmpty(), "Expected empty channel status list");
    }

    @Test
    void testConvertWithMultipleChannelStatuses() {
        NotificationAdminProto.ChannelEventStatus status1 = NotificationAdminProto.ChannelEventStatus.newBuilder()
                .setChannelType(NotificationAdminProto.ChannelType.Value.EMAIL)
                .setEventStatus(NotificationAdminProto.EventStatus.Value.PROCESSED)
                .build();
        NotificationAdminProto.ChannelEventStatus status2 = NotificationAdminProto.ChannelEventStatus.newBuilder()
                .setChannelType(NotificationAdminProto.ChannelType.Value.SLACK)
                .setEventStatus(NotificationAdminProto.EventStatus.Value.UNPROCESSED)
                .build();
        NotificationAdminProto.ChannelEventStatus status3 = NotificationAdminProto.ChannelEventStatus.newBuilder()
                .setChannelType(NotificationAdminProto.ChannelType.Value.IN_APP)
                .setEventStatus(NotificationAdminProto.EventStatus.Value.PROCESSED)
                .build();

        NotificationAdminProto.PublishedEventStatus eventStatus = NotificationAdminProto.PublishedEventStatus.newBuilder()
                .setPublishedEventId("pub-2")
                .setEventTypeId("event-type-2")
                .setTitle("Title Two")
                .setResourceCrn("crn:cdp:env:us-east-1:acct:environment:env2")
                .setTargetedEventType("TARGET_TYPE")
                .setDescription("Three channel statuses")
                .addStatus(status1)
                .addStatus(status2)
                .addStatus(status3)
                .setCreatedAt(99999L)
                .build();

        NotificationAdminProto.GetPublishedEventStatusResponse proto = NotificationAdminProto
                .GetPublishedEventStatusResponse.newBuilder()
                .setPublishedEventStatus(eventStatus)
                .build();

        GetPublishedEventStatusResponseDto dto = underTest.convert(proto);
        assertEquals("pub-2", dto.publishedEventId());
        assertEquals("event-type-2", dto.eventTypeId());
        assertEquals("Title Two", dto.title());
        assertEquals("crn:cdp:env:us-east-1:acct:environment:env2", dto.resourceCrn());
        assertEquals("TARGET_TYPE", dto.targetedEventType());
        assertEquals("Three channel statuses", dto.description());
        assertEquals(99999L, dto.createdAt());

        List<ChannelStatusDto> channels = dto.channelStatuses();
        assertNotNull(channels);
        assertEquals(3, channels.size());

        ChannelStatusDto c1 = channels.get(0);
        assertEquals("EMAIL", c1.channel());
        assertEquals("PROCESSED", c1.status());
        ChannelStatusDto c2 = channels.get(1);
        assertEquals("SLACK", c2.channel());
        assertEquals("UNPROCESSED", c2.status());
        ChannelStatusDto c3 = channels.get(2);
        assertEquals("IN_APP", c3.channel());
        assertEquals("PROCESSED", c3.status());
    }
}
