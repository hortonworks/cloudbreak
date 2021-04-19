package com.sequenceiq.datalake.flow.datalake.upgrade.handler;

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
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeImageChangeEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeValidationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeUpgradeWaitHandler extends ExceptionCatcherEventHandler<DatalakeUpgradeWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradeWaitHandler.class);

    @Value("${sdx.stack.upgrade.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.upgrade.duration_min:90}")
    private int durationInMinutes;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "DatalakeUpgradeWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeUpgradeWaitRequest> event) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeUpgradeWaitRequest> event) {
        DatalakeUpgradeWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster upgrade process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Stack Upgrade");
            response = new DatalakeImageChangeEvent(sdxId, userId, request.getImageId());
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Upgrade polling exited before timeout. Cause: ", userBreakException);
            if (userBreakException.getCause() instanceof UpgradeValidationFailedException) {
                response = new DatalakeUpgradeValidationFailedEvent(sdxId, userId);
            } else {
                response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
            }
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Upgrade poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake upgrade timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Upgrade polling failed for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, exception);
        }
        return response;
    }
}
