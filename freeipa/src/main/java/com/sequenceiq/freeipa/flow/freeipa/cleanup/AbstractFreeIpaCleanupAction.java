package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.failure.CleanupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

public abstract class AbstractFreeIpaCleanupAction<P extends CleanupEvent>
        extends AbstractStackAction<FreeIpaCleanupState, FreeIpaCleanupEvent, FreeIpaContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFreeIpaCleanupAction.class);

    @Inject
    private FreeIpaService freeIpaService;

    protected AbstractFreeIpaCleanupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected FreeIpaContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaCleanupState, FreeIpaCleanupEvent> stateContext,
            P payload) {
        FreeIpa freeIpa = freeIpaService.findByStackId(payload.getResourceId());
        MDCBuilder.addOperationId(payload.getOperationId());
        return new FreeIpaContext(flowParameters, freeIpa);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<FreeIpaContext> flowContext, Exception ex) {
        LOGGER.warn("Failure happened during cleanup, and 'getFailurePayload' is not overridden in state.", ex);
        return new CleanupFailureEvent(payload, "Unknown phase", Map.of(), Set.of());
    }

    protected boolean shouldSkipState(CleanupEvent event, Map<Object, Object> variables) {
        return event.getStatesToSkip() != null && event.getStatesToSkip().contains(getCurrentFlowStateName(variables));
    }
}
