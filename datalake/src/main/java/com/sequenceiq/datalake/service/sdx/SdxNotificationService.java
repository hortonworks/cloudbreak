package com.sequenceiq.datalake.service.sdx;

import java.util.Collection;
import java.util.Collections;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.controller.sdx.SdxClusterConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.notification.WebSocketNotificationService;

@Service
public class SdxNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxNotificationService.class);

    @Inject
    private WebSocketNotificationService webSocketNotificationService;

    @Inject
    private SdxClusterConverter sdxClusterConverter;

    public void send(ResourceEvent resourceEvent, SdxCluster sdx) {
        send(resourceEvent, Collections.singleton(sdx.getClusterName()), sdx, getUserCrn(sdx));
    }

    public void send(ResourceEvent resourceEvent, SdxCluster sdx, String userCrn) {
        send(resourceEvent, Collections.singleton(sdx.getClusterName()), sdx, userCrn);
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, SdxCluster sdx) {
        send(resourceEvent, messageArgs, sdx, getUserCrn(sdx));
    }

    public void send(ResourceEvent resourceEvent, Collection<?> messageArgs, SdxCluster sdx, String userCrn) {
        webSocketNotificationService.send(resourceEvent, messageArgs, sdxClusterConverter.sdxClusterToResponse(sdx), userCrn, null);
        LOGGER.info("SDX Notification has been sent: {}", resourceEvent);
    }

    private String getUserCrn(SdxCluster sdxCluster) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        boolean internalCrn = RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn);
        if (!internalCrn) {
            return userCrn;
        } else {
            return Crn.copyWithDifferentAccountId(Crn.safeFromString(userCrn), sdxCluster.getAccountId()).toString();
        }
    }

}
