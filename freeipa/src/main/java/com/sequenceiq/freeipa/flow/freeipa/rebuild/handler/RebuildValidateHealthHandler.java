package com.sequenceiq.freeipa.flow.freeipa.rebuild.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthFailed;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthRequest;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.health.RebuildValidateHealthSuccess;
import com.sequenceiq.freeipa.service.stack.FreeIpaSafeInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RebuildValidateHealthHandler extends ExceptionCatcherEventHandler<RebuildValidateHealthRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildValidateHealthHandler.class);

    @Inject
    private FreeIpaSafeInstanceHealthDetailsService healthService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RebuildValidateHealthRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RebuildValidateHealthRequest> event) {
        return new RebuildValidateHealthFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RebuildValidateHealthRequest> event) {
        RebuildValidateHealthRequest request = event.getData();
        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        List<NodeHealthDetails> healthDetails = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(im -> healthService.getInstanceHealthDetails(stack, im))
                .toList();
        LOGGER.info("Instance health details: {}", healthDetails);
        List<NodeHealthDetails> unhealthy = healthDetails.stream().filter(healthDetail -> !healthDetail.getStatus().isAvailable()).toList();
        if (unhealthy.isEmpty()) {
            return new RebuildValidateHealthSuccess(event.getData().getResourceId());
        } else {
            Exception exception = new Exception("Instance(s) healthcheck failed: " + unhealthy);
            return new RebuildValidateHealthFailed(request.getResourceId(), exception, VALIDATION);
        }
    }
}
