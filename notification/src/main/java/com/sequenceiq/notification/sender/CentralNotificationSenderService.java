package com.sequenceiq.notification.sender;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.sender.converter.NotificationDtosToCreateDistributionListRequestConverter;
import com.sequenceiq.notification.sender.dto.CreateDistributionListRequest;
import com.sequenceiq.notification.sender.dto.NotificationDto;
import com.sequenceiq.notification.sender.dto.NotificationSendingDtos;

@Service
public class CentralNotificationSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CentralNotificationSenderService.class);

    private final Map<ChannelType, NotificationSenderService> senderServiceMap;

    private final DistributionListManagementService distributionListManagementService;

    private final NotificationDtosToCreateDistributionListRequestConverter distributionListConverter;

    public CentralNotificationSenderService(
            List<NotificationSenderService> notificationSenderServices,
            DistributionListManagementService distributionListManagementService,
            NotificationDtosToCreateDistributionListRequestConverter distributionListConverter) {
        this.senderServiceMap = notificationSenderServices
                .stream()
                .collect(toMap(NotificationSenderService::channelType, gs -> gs));
        this.distributionListManagementService = distributionListManagementService;
        this.distributionListConverter = distributionListConverter;
    }

    // This is required to handle existing resources
    public List<DistributionList> processDistributionList(NotificationSendingDtos notificationSendingDtos) {
        List<DistributionList> distributionLists = List.of();
        Set<CreateDistributionListRequest> requests = distributionListConverter.convert(notificationSendingDtos);
        if (!requests.isEmpty()) {
            // Distribution list is returned if it is created or a System managed list gets updated
            distributionLists = distributionListManagementService.createOrUpdateLists(requests);
            LOGGER.debug("Created or updated distribution lists for {} resource CRNs", requests.size());
        } else {
            LOGGER.debug("No distribution list creation/update needed for provided notifications");
        }
        return distributionLists;
    }

    public List<NotificationDto> sendNotificationToDeliverySystem(NotificationSendingDtos notificationSendingDtos) {
        List<NotificationDto> notificationsSent = new ArrayList<>();
        notificationSendingDtos.notifications()
                .forEach(notification -> {
                    try {
                        NotificationSenderService notificationSenderService = senderServiceMap.get(notification.getChannelType());
                        if (notificationSenderService != null) {
                            LOGGER.debug("Sending notification {} to channel type {}", notification, notification.getChannelType());
                            notificationSenderService.send(notification);
                            notificationsSent.add(notification);
                        } else {
                            LOGGER.warn("No sender service found for channel type {}", notification.getChannelType());
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Sending have not happened for {} because {}", notification, e.getMessage(), e);
                    }
                });

        return notificationsSent;
    }

}
