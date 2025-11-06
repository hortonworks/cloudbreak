package com.sequenceiq.cloudbreak.notification.client.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto;
import com.sequenceiq.cloudbreak.notification.client.dto.ChannelTypeDto;
import com.sequenceiq.cloudbreak.notification.client.dto.EventChannelPreferenceDto;

/**
 * Converter for mapping between EventChannelPreferenceDto and protobuf EventChannelPreference.
 */
@Component
public class EventChannelPreferenceDtoConverter {

    /**
     * Converts an EventChannelPreferenceDto to a protobuf EventChannelPreference.
     *
     * @param dto the EventChannelPreferenceDto to convert
     * @return the corresponding protobuf EventChannelPreference
     */
    public NotificationAdminProto.EventChannelPreference convertToProto(EventChannelPreferenceDto dto) {
        if (dto == null) {
            return null;
        } else {
            NotificationAdminProto.EventChannelPreference.Builder builder = NotificationAdminProto.EventChannelPreference.newBuilder();
            if (dto.eventType() != null) {
                builder.setEventTypeId(dto.eventType());
            }

            if (dto.channelType() != null) {
                for (ChannelTypeDto channel : dto.channelType()) {
                    NotificationAdminProto.ChannelType.Value channelValue = convertChannelTypeDto(channel);
                    if (channelValue != null) {
                        builder.addChannel(channelValue);
                    }
                }
            }
            if (dto.eventSeverityList() != null) {
                builder.addAllEventSeverityList(dto.eventSeverityList());
            }
            return builder.build();
        }
    }

    private NotificationAdminProto.ChannelType.Value convertChannelTypeDto(ChannelTypeDto channelTypeDto) {
        if (channelTypeDto == null) {
            return NotificationAdminProto.ChannelType.Value.UNKNOWN;
        } else {
            return switch (channelTypeDto) {
                case EMAIL -> NotificationAdminProto.ChannelType.Value.EMAIL;
                case IN_APP -> NotificationAdminProto.ChannelType.Value.IN_APP;
                case SLACK -> NotificationAdminProto.ChannelType.Value.SLACK;
            };
        }
    }

}
