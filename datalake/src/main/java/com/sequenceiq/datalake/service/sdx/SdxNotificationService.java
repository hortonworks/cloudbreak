package com.sequenceiq.datalake.service.sdx;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.controller.sdx.SdxClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.notification.NotificationService;

@Service
public class SdxNotificationService {

    @Inject
    private NotificationService notificationService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    public void send(ResourceEvent resourceEvent, SdxCluster sdx) {
        notificationService.send(resourceEvent, sdxClusterConverter.sdxClusterToResponse(sdx), sdx.getInitiatorUserCrn());
    }
}
