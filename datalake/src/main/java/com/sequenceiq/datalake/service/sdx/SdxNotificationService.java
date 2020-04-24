package com.sequenceiq.datalake.service.sdx;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.controller.sdx.SdxClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.notification.NotificationService;

@Service
public class SdxNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxNotificationService.class);

    @Inject
    private NotificationService notificationService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    public void send(ResourceEvent resourceEvent, SdxCluster sdx) {
        LOGGER.info("SDX Notification has been sent: {}", resourceEvent);
        notificationService.send(resourceEvent, sdxClusterConverter.sdxClusterToResponse(sdx), sdx.getInitiatorUserCrn());
    }
}
