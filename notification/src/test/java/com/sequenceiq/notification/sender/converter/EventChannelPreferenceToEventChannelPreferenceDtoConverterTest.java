package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.notification.client.dto.ChannelTypeDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationSeverity;

@ExtendWith(MockitoExtension.class)
public class EventChannelPreferenceToEventChannelPreferenceDtoConverterTest {

    @Mock
    private NotificationSeverityConverter notificationSeverityConverter;

    @InjectMocks
    private EventChannelPreferenceToEventChannelPreferenceDtoConverter underTest;

    @Test
    @DisplayName("convert(EventChannelPreference) null -> null")
    void testConvertDomainNull() {
        assertNull(underTest.convert((EventChannelPreference) null));
    }

    @Test
    @DisplayName("convert(EventChannelPreference) maps all channel types & severities; filters null channel type")
    void testConvertDomainToDto() {
        Set<ChannelType> channelTypes = new HashSet<>();
        channelTypes.add(ChannelType.EMAIL);
        channelTypes.add(ChannelType.IN_APP);
        channelTypes.add(ChannelType.SLACK);
        channelTypes.add(null);

        Set<NotificationSeverity> severities = Set.of(NotificationSeverity.INFO, NotificationSeverity.WARNING);

        EventChannelPreference preference = new EventChannelPreference("EVENT_A", channelTypes, severities);
        EventChannelPreferenceDto dto = underTest.convert(preference);

        assertNotNull(dto);
        assertEquals("EVENT_A", dto.eventType());
        assertEquals(Set.of(ChannelTypeDto.EMAIL, ChannelTypeDto.IN_APP, ChannelTypeDto.SLACK), dto.channelType());
        assertEquals(Set.of("INFO", "WARNING"), dto.eventSeverityList());
    }

    @Test
    @DisplayName("convert(List<EventChannelPreference>) handles null list, empty list, and filters null element")
    void testConvertListVariants() {
        EventChannelPreference p1 = new EventChannelPreference("EV1", Set.of(ChannelType.EMAIL), Set.of(NotificationSeverity.DEBUG));
        EventChannelPreference p2 = new EventChannelPreference("EV2", Set.of(ChannelType.SLACK), Set.of(NotificationSeverity.ERROR));
        List<EventChannelPreference> listWithNull = new ArrayList<>();
        listWithNull.add(p1);
        listWithNull.add(null);
        listWithNull.add(p2);

        List<EventChannelPreferenceDto> converted = underTest.convert(listWithNull);
        assertEquals(2, converted.size());
        assertEquals(Set.of("DEBUG"), converted.get(0).eventSeverityList());
        assertEquals(Set.of("ERROR"), converted.get(1).eventSeverityList());

        assertTrue(underTest.convert((List<EventChannelPreference>) null).isEmpty());
        assertTrue(underTest.convert(List.of()).isEmpty());
    }

    @Test
    @DisplayName("convert(EventChannelPreference) with null channelType & severities -> dto with null sets")
    void testConvertDomainWithNullSets() {
        EventChannelPreference preference = new EventChannelPreference("EV_NULL", null, null);
        EventChannelPreferenceDto dto = underTest.convert(preference);
        assertNotNull(dto);
        assertEquals("EV_NULL", dto.eventType());
        assertNotNull(dto.channelType());
        assertNotNull(dto.eventSeverityList());
    }
}

