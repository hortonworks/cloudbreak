package com.sequenceiq.notification.domain;

import java.util.Set;

public record EventChannelPreference(
        String eventType,
        Set<ChannelType> channelType,
        Set<NotificationSeverity> eventSeverityList
) {
}