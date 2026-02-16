package com.sequenceiq.notification.sender.converter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.notification.client.dto.ChannelTypeDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;
import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationSeverity;

@Component
public class EventChannelPreferenceToEventChannelPreferenceDtoConverter {

    @Inject
    private NotificationSeverityConverter notificationSeverityConverter;

    public EventChannelPreferenceDto convert(EventChannelPreference preference) {
        if (preference == null) {
            return null;
        } else {
            Set<ChannelTypeDto> channelTypes = new HashSet<>();
            Set<String> eventSeverityList = new HashSet<>();
            if (preference.channelType() != null) {
                channelTypes.addAll(
                        preference.channelType().stream()
                                .map(this::convertChannelType)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                );
            }
            if (preference.eventSeverityList() != null) {
                eventSeverityList.addAll(preference.eventSeverityList().stream()
                        .map(NotificationSeverity::name)
                        .collect(Collectors.toSet()));
            }
            return new EventChannelPreferenceDto(preference.eventType(), channelTypes, eventSeverityList);
        }
    }

    public List<EventChannelPreferenceDto> convert(List<EventChannelPreference> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return Collections.emptyList();
        } else {
            return preferences.stream()
                    .map(this::convert)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private ChannelTypeDto convertChannelType(ChannelType channelType) {
        if (channelType == null) {
            return null;
        } else {
            return switch (channelType) {
                case EMAIL -> ChannelTypeDto.EMAIL;
                case LOCAL_EMAIL -> ChannelTypeDto.LOCAL_EMAIL;
                case IN_APP -> ChannelTypeDto.IN_APP;
                case SLACK -> ChannelTypeDto.SLACK;
            };
        }
    }
}

