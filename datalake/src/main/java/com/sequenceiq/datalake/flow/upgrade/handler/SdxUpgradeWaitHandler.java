package com.sequenceiq.datalake.flow.upgrade.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeSuccessEvent;
import com.sequenceiq.datalake.flow.upgrade.event.SdxUpgradeWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class SdxUpgradeWaitHandler implements EventHandler<SdxUpgradeWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 60;

    @Inject
    private EventBus eventBus;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "SdxUpgradeWaitRequest";
    }

    @Override
    public void accept(Event<SdxUpgradeWaitRequest> event) {
        SdxUpgradeWaitRequest request = event.getData();
        MDCBuilder.addRequestId(request.getRequestId());
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        String requestId = request.getRequestId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster upgrade process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "Upgrade");
            response = new SdxUpgradeSuccessEvent(sdxId, userId, requestId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Upgrade polling exited before timeout. Cause: ", userBreakException);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Upgrade poller stopped for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId,
                    new PollerStoppedException("Datalake repair timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Upgrade polling failed for cluster: {}", sdxId);
            response = new SdxUpgradeFailedEvent(sdxId, userId, requestId, exception);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
