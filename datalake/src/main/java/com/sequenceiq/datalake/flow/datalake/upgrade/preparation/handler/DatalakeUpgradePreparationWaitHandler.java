package com.sequenceiq.datalake.flow.datalake.upgrade.preparation.handler;

import static com.sequenceiq.datalake.flow.datalake.upgrade.preparation.DatalakeUpgradePreparationEvent.DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.preparation.event.DatalakeUpgradePreparationWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeUpgradePreparationWaitHandler extends ExceptionCatcherEventHandler<DatalakeUpgradePreparationWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradePreparationWaitHandler.class);

    @Value("${sdx.stack.upgrade.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.upgrade.duration_min:120}")
    private int durationInMinutes;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeUpgradePreparationWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeUpgradePreparationWaitRequest> event) {
        LOGGER.error("Upgrade preparation polling failed unexpectedly for cluster: {}", resourceId, e);
        return new DatalakeUpgradePreparationFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeUpgradePreparationWaitRequest> event) {
        Long sdxId = event.getData().getResourceId();
        String userId = event.getData().getUserId();
        try {
            LOGGER.info("Start polling cluster upgrade preparation process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Cluster Upgrade preparation");
            return new SdxEvent(DATALAKE_UPGRADE_PREPARATION_SUCCESS_EVENT.event(), sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Upgrade polling exited before timeout. Cause: ", userBreakException);
            return new DatalakeUpgradePreparationFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Upgrade poller stopped for cluster: {}", sdxId);
            return new DatalakeUpgradePreparationFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake upgrade preparation timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Upgrade preparation polling failed for cluster: {}", sdxId);
            return new DatalakeUpgradePreparationFailedEvent(sdxId, userId, exception);
        }
    }
}
