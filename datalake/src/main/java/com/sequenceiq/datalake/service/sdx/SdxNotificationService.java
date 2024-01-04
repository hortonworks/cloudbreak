package com.sequenceiq.datalake.service.sdx;

import java.util.Collection;
import java.util.Collections;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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
        send(resourceEvent, Collections.singleton(sdx.getClusterName()), sdx);
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, SdxCluster sdx) {
        notificationService.send(resourceEvent, messageArgs, sdxClusterConverter.sdxClusterToResponse(sdx),
                getAccountIdFromSdx(sdx));
        LOGGER.info("SDX Notification has been sent: {}", resourceEvent);
    }

    private String getAccountIdFromSdx(SdxCluster sdx) {
        return (sdx.getAccountId() != null) ? sdx.getAccountId() : ThreadBasedUserCrnProvider.getUserCrn();
    }
}
