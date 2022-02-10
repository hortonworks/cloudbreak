package com.sequenceiq.datalake.flow.upgrade.ccm.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmStackRequest;
import com.sequenceiq.datalake.flow.upgrade.ccm.event.UpgradeCcmSuccessEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.upgrade.ccm.SdxCcmUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class UpgradeCcmStackHandler extends ExceptionCatcherEventHandler<UpgradeCcmStackRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmStackHandler.class);

    @Value("${sdx.upgrade.ccm.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.upgrade.ccm.duration_min:120}")
    private int durationInMinutes;

    @Inject
    private SdxCcmUpgradeService ccmUpgradeService;

    @Override
    public String selector() {
        return UpgradeCcmStackRequest.class.getSimpleName();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeCcmStackRequest> event) {
        return new UpgradeCcmFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeCcmStackRequest> event) {
        UpgradeCcmStackRequest request = event.getData();
        SdxCluster sdxCluster = request.getSdxCluster();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Initiating CCM upgrade and start polling for SDX: {}", sdxCluster.getName());
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            ccmUpgradeService.initAndWaitForStackUpgrade(sdxCluster, pollingConfig);
            response = new UpgradeCcmSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Upgrade CCM poller exited before timeout. Cause: ", userBreakException);
            response = new UpgradeCcmFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Upgrade CCM poller stopped for stack: {}", sdxId);
            response = new UpgradeCcmFailedEvent(sdxId, userId,
                    new PollerStoppedException("Upgrade CCM timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Upgrade CCM polling failed for stack: {}", sdxId);
            response = new UpgradeCcmFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
