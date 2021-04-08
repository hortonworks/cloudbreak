package com.sequenceiq.datalake.flow.diagnostics.handler;

import java.util.Map;
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
import com.sequenceiq.datalake.flow.diagnostics.SdxDiagnosticsFlowService;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsFailedEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsSuccessEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsWaitRequest;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxDiagnosticsCollectionHandler extends ExceptionCatcherEventHandler<SdxDiagnosticsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDiagnosticsCollectionHandler.class);

    @Value("${sdx.diagnostics.collection.sleeptime_sec:5}")
    private int sleepTimeInSec;

    @Value("${sdx.diagnostics.collection.duration_min:20}")
    private int durationInMinutes;

    @Inject
    private SdxDiagnosticsFlowService diagnosticsFlowService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxDiagnosticsWaitRequest> event) {
        return new SdxDiagnosticsFailedEvent(resourceId, null, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxDiagnosticsWaitRequest> event) {
        SdxDiagnosticsWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Map<String, Object> properties = request.getProperties();
        Selectable response;
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            diagnosticsFlowService.waitForDiagnosticsCollection(sdxId, pollingConfig, request.getFlowIdentifier());
            response = new SdxDiagnosticsSuccessEvent(sdxId, userId, properties);
            LOGGER.debug("SDX diagnostics collection event finished");
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout for SDX (diagnostic collection): {}. Cause: ", sdxId, userBreakException);
            response = new SdxDiagnosticsFailedEvent(sdxId, userId, properties, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for SDX (diagnostic collection): {}", sdxId, pollerStoppedException);
            response = new SdxDiagnosticsFailedEvent(sdxId, userId, properties,
                    new PollerStoppedException("Datalake diagnostic collection timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for stack (diagnostic collection): {}", sdxId, exception);
            response = new SdxDiagnosticsFailedEvent(sdxId, userId, properties, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in diagnostic collection wait phase", anotherException);
            response = new SdxDiagnosticsFailedEvent(sdxId, userId, properties, anotherException);
        }
        return response;
    }

    @Override
    public String selector() {
        return "SdxDiagnosticsWaitRequest";
    }

}
