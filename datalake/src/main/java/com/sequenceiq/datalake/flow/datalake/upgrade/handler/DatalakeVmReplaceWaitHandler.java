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
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeSuccessEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeVmReplaceWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeVmReplaceWaitHandler extends ExceptionCatcherEventHandler<DatalakeVmReplaceWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeVmReplaceWaitHandler.class);

    @Value("${sdx.vm.replace.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.vm.replace.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxUpgradeService upgradeService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeVmReplaceWaitRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeVmReplaceWaitRequest> event) {
        return new DatalakeUpgradeFailedEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeVmReplaceWaitRequest> event) {
        DatalakeVmReplaceWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Selectable response;
        try {
            LOGGER.info("Start polling cluster VM replacement process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            upgradeService.waitCloudbreakFlow(sdxId, pollingConfig, "VM replace");
            response = new DatalakeUpgradeSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.error("VM replace polling exited before timeout. Cause: ", userBreakException);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("VM replace poller stopped for cluster: {}", sdxId);
            response = new DatalakeUpgradeFailedEvent(sdxId, userId,
                    new PollerStoppedException("VM replace timed out after " + durationInMinutes + " minutes"));
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
