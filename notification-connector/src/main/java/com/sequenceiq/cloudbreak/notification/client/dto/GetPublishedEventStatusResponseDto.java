package com.sequenceiq.cloudbreak.notification.client.dto;

import java.util.List;

public record GetPublishedEventStatusResponseDto(
        String publishedEventId,
        String eventTypeId,
        String title,
        String resourceCrn,
        String targetedEventType,
        String description,
        List<ChannelStatusDto> channelStatuses,
        long createdAt
) {
    public record ChannelStatusDto(
            String channel,
            String status
    ) {
    }
}