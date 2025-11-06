package com.sequenceiq.cloudbreak.notification.client.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto;
import com.sequenceiq.cloudbreak.notification.client.dto.GetPublishedEventStatusResponseDto.ChannelStatusDto;

@Component
public class GetPublishedEventStatusResponseConverter {

    public GetPublishedEventStatusResponseDto convert(NotificationAdminProto.GetPublishedEventStatusResponse proto) {
        if (proto == null) {
            return new GetPublishedEventStatusResponseDto(null, null, null, null, null, null, List.of(), 0L);
        } else {
            NotificationAdminProto.PublishedEventStatus eventStatus = proto.getPublishedEventStatus();

            List<ChannelStatusDto> channelStatuses = eventStatus.getStatusList()
                    .stream()
                    .map(this::convertChannelStatus)
                    .collect(Collectors.toList());

            return new GetPublishedEventStatusResponseDto(
                    eventStatus.getPublishedEventId(),
                    eventStatus.getEventTypeId(),
                    eventStatus.getTitle(),
                    eventStatus.getResourceCrn(),
                    eventStatus.getTargetedEventType(),
                    eventStatus.getDescription(),
                    channelStatuses,
                    eventStatus.getCreatedAt());
        }
    }

    private ChannelStatusDto convertChannelStatus(NotificationAdminProto.ChannelEventStatus channelStatus) {
        return new ChannelStatusDto(
                channelStatus.getChannelType().name(),
                channelStatus.getEventStatus().name()
        );
    }
}
