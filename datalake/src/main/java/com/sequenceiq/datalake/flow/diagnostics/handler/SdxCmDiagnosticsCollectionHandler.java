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
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsFailedEvent;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxCmDiagnosticsWaitRequest;
import com.sequenceiq.datalake.flow.diagnostics.event.SdxDiagnosticsSuccessEvent;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxCmDiagnosticsCollectionHandler extends ExceptionCatcherEventHandler<SdxCmDiagnosticsWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCmDiagnosticsCollectionHandler.class);

    @Value("${sdx.diagnostics.cm.collection.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.diagnostics.cm.collection.duration_min:60}")
    private int durationInMinutes;

    @Inject
    private SdxDiagnosticsFlowService diagnosticsFlowService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxCmDiagnosticsWaitRequest> event) {
        return new SdxCmDiagnosticsFailedEvent(resourceId, null, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxCmDiagnosticsWaitRequest> event) {
        SdxCmDiagnosticsWaitRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        Map<String, Object> properties = request.getProperties();
        Selectable response;
        try {
            PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
            diagnosticsFlowService.waitForDiagnosticsCollection(sdxId, pollingConfig, request.getFlowIdentifier(), true);
            response = new SdxDiagnosticsSuccessEvent(sdxId, userId, properties);
            LOGGER.debug("SDX CM based diagnostics collection event finished");
        } catch (UserBreakException userBreakException) {
            LOGGER.error("Polling exited before timeout for SDX (CM based diagnostic collection): {}. Cause: ", sdxId, userBreakException);
            response = new SdxCmDiagnosticsFailedEvent(sdxId, userId, properties, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.error("Poller stopped for SDX (CM based diagnostic collection): {}", sdxId, pollerStoppedException);
            response = new SdxCmDiagnosticsFailedEvent(sdxId, userId, properties,
                    new PollerStoppedException("Datalake CM based diagnostic collection timed out after " + durationInMinutes + " minutes"));
        } catch (PollerException exception) {
            LOGGER.error("Polling failed for stack (CM based diagnostic collection): {}", sdxId, exception);
            response = new SdxCmDiagnosticsFailedEvent(sdxId, userId, properties, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in CM based diagnostic collection wait phase", anotherException);
            response = new SdxCmDiagnosticsFailedEvent(sdxId, userId, properties, anotherException);
        }
        return response;
    }

    @Override
    public String selector() {
        return "SdxCmDiagnosticsWaitRequest";
    }

}
