package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.HashSet;
import java.util.Set;

public record EventChannelPreferenceDto(
        String eventType,
        Set<ChannelTypeDto> channelType,
        Set<String> eventSeverityList
) {
    public EventChannelPreferenceDto() {
        this(null, new HashSet<>(), new HashSet<>());
    }
}