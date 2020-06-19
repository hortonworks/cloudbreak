package com.sequenceiq.datalake.flow.datalake.upgrade.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeImageChangeEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class DatalakeUpgradeWaitHandler extends ExceptionCatcherEventHandler<DatalakeUpgradeWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeUpgradeWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 90;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "DatalakeUpgradeWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        DatalakeUpgradeWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster upgrade process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Stack Upgrade");
            response = new DatalakeImageChangeEvent(sdxId, userId, request.getImageId());
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Upgrade polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Upgrade poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake upgrade timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Upgrade polling failed for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }
}
