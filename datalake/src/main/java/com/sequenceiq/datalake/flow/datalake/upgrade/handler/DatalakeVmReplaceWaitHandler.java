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
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeSuccessEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeVmReplaceWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class DatalakeVmReplaceWaitHandler extends ExceptionCatcherEventHandler<DatalakeVmReplaceWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeVmReplaceWaitHandler.class);

    private static final int SLEEP_TIME_IN_SEC = 20;

    private static final int DURATION_IN_MINUTES = 60;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return "DatalakeVmReplaceWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeVmReplaceWaitRequest> event) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        DatalakeVmReplaceWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster VM replacement process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "VM replace");
            response = new DatalakeUpgradeSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("VM replace polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("VM replace poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("VM replace timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("VM replace polling failed for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened during VM replacement wait phase", anotherException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, anotherException);
        }

        return response;
    }
}
