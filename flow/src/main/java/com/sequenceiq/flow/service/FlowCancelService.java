package com.sequenceiq.flow.service;

import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Service
public class FlowCancelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCancelService.class);

    private static final long MINUTE_IN_MS = 60 * 1000;

    @Inject
    private EventBus reactor;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private EventParameterFactory eventParameterFactory;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private Flow2Handler flow2Handler;

    @Value("${cb.termination.retry.allowed.in.minute:60}")
    private long terminationRetryAllowedInMinute;

    public void cancelRunningFlows(Long resourceId) {
        LOGGER.info("Cancel running flow for id: [{}]", resourceId);
        Payload cancelEvent = () -> resourceId;
        reactor.notify(Flow2Handler.FLOW_CANCEL, eventFactory.createEventWithErrHandler(eventParameterFactory.createEventParameters(resourceId), cancelEvent));
    }

    public void cancelFlowSilently(FlowLog flowLog) {
        try {
            cancelFlow(flowLog);
        } catch (TransactionExecutionException e) {
            LOGGER.error("Couldn't cancel flow [{}] for resource [{}]. FlowType: [{}] Current state: [{}]",
                    flowLog.getFlowId(), flowLog.getResourceId(), flowLog.getFlowType(), flowLog.getCurrentState(), e);
        }
    }

    public void cancelFlow(FlowLog flowLog) throws TransactionExecutionException {
        LOGGER.debug("Cancel flow [{}] for resource [{}]. FlowType: [{}] Current state: [{}]",
                flowLog.getFlowId(), flowLog.getResourceId(), flowLog.getFlowType(), flowLog.getCurrentState());
        flow2Handler.cancelFlow(flowLog.getResourceId(), flowLog.getFlowId());
    }

    public void cancelTooOldTerminationFlowForResource(Long stackId, String stackName) {
        try {
            flowLogService.cancelTooOldTerminationFlowForResource(stackId, new Date().getTime() - terminationRetryAllowedInMinute * MINUTE_IN_MS);
        } catch (Exception e) {
            LOGGER.warn("Can't cancel old termination flows for stack {}", stackName, e);
        }
    }
}
