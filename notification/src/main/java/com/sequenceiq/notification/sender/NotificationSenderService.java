package com.sequenceiq.notification.sender;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.sender.dto.NotificationDto;

public interface NotificationSenderService {

    void send(NotificationDto no);

    ChannelType channelType();
}
