package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.Set;

public record EventChannelPreferenceDto(
        String eventType,
        Set<ChannelTypeDto> channelType,
        Set<String> eventSeverityList
) {
}