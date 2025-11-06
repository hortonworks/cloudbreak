package com.sequenceiq.cloudbreak.notification.client.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.ChannelTypeDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;

class EventChannelPreferenceDtoConverterTest {

    private final EventChannelPreferenceDtoConverter underTest = new EventChannelPreferenceDtoConverter();

    @Test
    void testConvertToProtoNullInput() {
        assertNull(underTest.convertToProto(null));
    }

    @Test
    void testConvertToProtoWithAllFieldsAndAllChannelVariants() {
        Set<ChannelTypeDto> channels = new LinkedHashSet<>();
        channels.add(ChannelTypeDto.EMAIL);
        channels.add(null);
        channels.add(ChannelTypeDto.IN_APP);
        channels.add(ChannelTypeDto.SLACK);

        Set<String> severities = Set.of("CRITICAL", "INFO");
        String eventType = "event-type-A";
        EventChannelPreferenceDto dto = new EventChannelPreferenceDto(eventType, channels, severities);

        NotificationAdminProto.EventChannelPreference proto = underTest.convertToProto(dto);
        assertNotNull(proto);
        assertEquals(eventType, proto.getEventTypeId());

        // Validate channels mapping. We expect EMAIL, IN_APP, SLACK and two UNKNOWN (from null + CONSOLE)
        List<NotificationAdminProto.ChannelType.Value> protoChannels = proto.getChannelList();
        assertEquals(4, protoChannels.size(), "All channel entries including duplicates for UNKNOWN should be present");
        assertTrue(protoChannels.contains(NotificationAdminProto.ChannelType.Value.EMAIL));
        assertTrue(protoChannels.contains(NotificationAdminProto.ChannelType.Value.IN_APP));
        assertTrue(protoChannels.contains(NotificationAdminProto.ChannelType.Value.SLACK));
        long unknownCount = protoChannels.stream().filter(v -> v == NotificationAdminProto.ChannelType.Value.UNKNOWN).count();
        assertEquals(1, unknownCount, "UNKNOWN should appear");

        // Validate severities
        List<String> protoSeverities = proto.getEventSeverityListList();
        assertEquals(2, protoSeverities.size());
        assertTrue(protoSeverities.containsAll(severities));
    }

    @Test
    void testConvertToProtoWithNullOptionalFields() {
        EventChannelPreferenceDto dto = new EventChannelPreferenceDto();
        // All fields default null
        NotificationAdminProto.EventChannelPreference proto = underTest.convertToProto(dto);
        assertNotNull(proto);
        // eventType not set => default empty string
        assertTrue(proto.getEventTypeId().isEmpty());
        assertEquals(0, proto.getChannelCount());
        assertEquals(0, proto.getEventSeverityListCount());
    }
}