package com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.resetjvmparams.ResetJvmParamsFlowEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ResetJvmParamsHandler extends ExceptionCatcherEventHandler<ResetJvmParamsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResetJvmParamsHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ResetJvmParamsRequest> event) {
        LOGGER.error("Reset JVM params failed.", e);
        return new ResetJvmParamsFailedEvent(ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ResetJvmParamsRequest> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        try {
            LOGGER.info("Resetting JVM params for stack {}", stackId);
            connector.reallocateMemory(true);
        } catch (Exception e) {
            LOGGER.error("Reset JVM params failed for stack {}.", stackId, e);
            return new ResetJvmParamsFailedEvent(ResetJvmParamsFlowEvent.RESET_JVM_PARAMS_FAILED_EVENT.event(), stackId, e);
        }
        return new ResetJvmParamsResult(event.getData());
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ResetJvmParamsRequest.class);
    }
}
