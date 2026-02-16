package com.sequenceiq.notification.sender.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.EventChannelPreference;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;

@Component
public class NotificationDtosToCreateDistributionListRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDtosToCreateDistributionListRequestConverter.class);

    public Set<CreateDistributionListRequest> convert(NotificationSendingDtos notificationSendingDtos) {
        if (notificationSendingDtos == null || CollectionUtils.isEmpty(notificationSendingDtos.notifications())) {
            LOGGER.debug("No notifications found for notification sending dto: {}", notificationSendingDtos);
            return Collections.emptySet();
        } else {
            Map<String, List<NotificationDto>> groupedNotifications = notificationSendingDtos.notifications()
                    .stream()
                    .filter(e -> !e.getChannelType().equals(ChannelType.LOCAL_EMAIL))
                    .filter(Objects::nonNull)
                    .filter(notificationDto -> notificationDto.isValidNotification(NotificationFormFactor.DISTRIBUTION_LIST))
                    .collect(Collectors.groupingBy(NotificationDto::getResourceCrn));

            LOGGER.debug("Grouped notifications into {} resource CRNs for distribution list processing", groupedNotifications.size());

            Set<CreateDistributionListRequest> distributionListRequests = groupedNotifications.entrySet().stream()
                    .map(this::createRequest)
                    .collect(Collectors.toSet());
            LOGGER.debug("Created distribution list requests {}", distributionListRequests);
            return distributionListRequests;
        }
    }

    private CreateDistributionListRequest createRequest(Map.Entry<String, List<NotificationDto>> entry) {
        String resourceCrn = entry.getKey();
        List<NotificationDto> dtos = entry.getValue();

        String resourceName = dtos.stream()
                .map(NotificationDto::getResourceName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Map<String, AggregatedPreference> aggregatedPreferences = new HashMap<>();
        for (NotificationDto dto : dtos) {
            NotificationType type = dto.getType();
            if (type != null) {
                String eventTypeId = type.getEventTypeId();
                AggregatedPreference preference = aggregatedPreferences.computeIfAbsent(eventTypeId, k -> new AggregatedPreference());
                Optional.ofNullable(dto.getChannelType()).ifPresent(preference.channelTypes::add);

                NotificationSeverity severity = Optional.ofNullable(dto.getSeverity()).orElse(type.getNotificationSeverity());
                Optional.ofNullable(severity).ifPresent(preference.severities::add);
            }
        }

        List<EventChannelPreference> preferences = aggregatedPreferences.entrySet().stream()
                .map(e -> new EventChannelPreference(e.getKey(), e.getValue().channelTypes, e.getValue().severities))
                .collect(Collectors.toList());

        return new CreateDistributionListRequest(resourceCrn, resourceName, preferences);
    }

    private static class AggregatedPreference {

        private final Set<ChannelType> channelTypes = new HashSet<>();

        private final Set<NotificationSeverity> severities = new HashSet<>();
    }
}
