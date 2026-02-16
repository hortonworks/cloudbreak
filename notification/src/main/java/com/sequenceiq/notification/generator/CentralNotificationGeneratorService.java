package com.sequenceiq.notification.generator;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.sender.LocalEmailProvider;

@Service
public class CentralNotificationGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralNotificationGeneratorService.class);

    private final Map<ChannelType, NotificationGeneratorService> generatorServiceMap;

    private final LocalEmailProvider localEmailProvider;

    public CentralNotificationGeneratorService(
            List<NotificationGeneratorService> notificationGeneratorServices,
            LocalEmailProvider localEmailProvider
    ) {
        this.localEmailProvider = localEmailProvider;
        this.generatorServiceMap = new HashMap<>();
        for (NotificationGeneratorService notificationGeneratorService : notificationGeneratorServices) {
            for (ChannelType channelType : notificationGeneratorService.channelTypes()) {
                generatorServiceMap.put(channelType, notificationGeneratorService);
            }
        }
    }

    public <T extends BaseNotificationRegisterAdditionalDataDtos> NotificationGeneratorDto generateNotification(
            NotificationGeneratorDto<T> dto,
            NotificationType notificationType
    ) {
        NotificationGeneratorDto generatorDto = NotificationGeneratorDto.builder()
                .notificationGeneratorDto(dto)
                .channelMessages(generateChannelMessages(dto, notificationType))
                .build();
        LOGGER.debug("Generated notification: {}", generatorDto);
        return generatorDto;
    }

    private Map<ChannelType, String> generateChannelMessages(NotificationGeneratorDto dto, NotificationType notificationType) {
        Set<ChannelType> channelTypes = localEmailProvider.getLocalChannelIfConfigured(notificationType.getChannelTypes());
        return channelTypes.stream()
                .map(channelType -> Map.entry(
                        channelType,
                        generatorServiceMap.get(channelType).generate(dto, notificationType).orElse(null)
                ))
                .filter(e -> e.getValue() != null)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
