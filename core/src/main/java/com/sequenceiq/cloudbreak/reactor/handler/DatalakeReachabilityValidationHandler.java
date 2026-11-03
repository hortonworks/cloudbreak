package com.sequenceiq.cloudbreak.reactor.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.DatalakeReachabilityValidationResult;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DatalakeReachabilityValidationHandler extends ExceptionCatcherEventHandler<DatalakeReachabilityValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeReachabilityValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DataLakeStatusCheckerService dataLakeStatusCheckerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeReachabilityValidationRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeReachabilityValidationRequest> event) {
        LOGGER.warn("Data Lake reachability validation failed", e);
        return new StackFailureEvent(EventSelectorUtil.failureSelector(DatalakeReachabilityValidationResult.class), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeReachabilityValidationRequest> event) {
        Long stackId = event.getData().getResourceId();
        dataLakeStatusCheckerService.validateDatalakeReachable(stackDtoService.getStackViewById(stackId));
        return new DatalakeReachabilityValidationResult(stackId);
    }
}
