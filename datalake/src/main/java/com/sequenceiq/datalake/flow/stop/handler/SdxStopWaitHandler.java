package com.sequenceiq.datalake.flow.stop.handler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.stop.event.SdxStopFailedEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopSuccessEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStopWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.stop.SdxStopService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class SdxStopWaitHandler extends ExceptionCatcherEventHandler<SdxStopWaitRequest> {

    public static final int SLEEP_TIME_IN_SEC = 20;

    public static final int DURATION_IN_MINUTES = 40;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxStopWaitHandler.class);

    @Inject
    private SdxStopService sdxStopService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxStopFailedEvent(resourceId, null, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        SdxStopWaitRequest waitRequest = event.getData();
        Long sdxId = waitRequest.getResourceId();
        String userId = waitRequest.getUserId();
        Selectable response;
        try {
            LOGGER.debug("Stop polling stack stopping process for id: {}", sdxId);
            PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC, TimeUnit.SECONDS, DURATION_IN_MINUTES, TimeUnit.MINUTES);
            sdxStopService.waitCloudbreakCluster(sdxId, pollingConfig);
            response = new SdxStopSuccessEvent(sdxId, userId);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Stop polling exited before timeout. Cause: ", userBreakException);
            response = new SdxStopFailedEvent(sdxId, userId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Stop poller stopped for stack: {}", sdxId);
            response = new SdxStopFailedEvent(sdxId, userId,
                    new PollerStoppedException("Datalake stop timed out after " + DURATION_IN_MINUTES + " minutes"));
        } catch (PollerException exception) {
            LOGGER.info("Stop polling failed for stack: {}", sdxId);
            response = new SdxStopFailedEvent(sdxId, userId, exception);
        }
        sendEvent(response, event);
    }

    @Override
    public String selector() {
        return "SdxStopWaitRequest";
    }
}
