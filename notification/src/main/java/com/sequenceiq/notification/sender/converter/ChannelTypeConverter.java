package com.sequenceiq.notification.sender.converter;

import static com.sequenceiq.notification.domain.ChannelType.EMAIL;
import static com.sequenceiq.notification.domain.ChannelType.IN_APP;
import static com.sequenceiq.notification.domain.ChannelType.SLACK;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ChannelType;

@Component
public class ChannelTypeConverter {

    public ChannelType.Value convert(com.sequenceiq.notification.domain.ChannelType channelType) {
        if (channelType == null) {
            return ChannelType.Value.UNKNOWN;
        }

        return switch (channelType) {
            case EMAIL, LOCAL_EMAIL -> ChannelType.Value.EMAIL;
            case IN_APP -> ChannelType.Value.IN_APP;
            case SLACK -> ChannelType.Value.SLACK;
        };
    }

    public com.sequenceiq.notification.domain.ChannelType convert(ChannelType.Value protoChannelType) {
        if (protoChannelType == null) {
            return null;
        }

        return switch (protoChannelType) {
            case EMAIL -> EMAIL;
            case IN_APP -> IN_APP;
            case SLACK -> SLACK;
            case UNKNOWN, UNRECOGNIZED -> null;
        };
    }
}
