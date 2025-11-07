package com.sequenceiq.notification.generator;

import java.util.Optional;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;

public interface NotificationGeneratorService {

    Optional<String> generate(NotificationGeneratorDto notificationGeneratorDto, NotificationType notificationType);

    ChannelType channelType();
}
