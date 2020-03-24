package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;

public abstract class AbstractFreeIpaCleanupAction<P extends Payload> extends AbstractStackAction<FreeIpaCleanupState, FreeIpaCleanupEvent, FreeIpaContext, P> {

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private CleanupService cleanupService;

    protected AbstractFreeIpaCleanupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected FreeIpaContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaCleanupState, FreeIpaCleanupEvent> stateContext,
            P payload) {
        FreeIpa freeIpa = freeIpaService.findByStackId(payload.getResourceId());
        return new FreeIpaContext(flowParameters, freeIpa);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<FreeIpaContext> flowContext, Exception ex) {
        return null;
    }

    protected CleanupService getCleanupService() {
        return cleanupService;
    }

    protected boolean shouldSkipState(CleanupEvent event, Map<Object, Object> variables) {
        return event.getStatesToSkip() != null && event.getStatesToSkip().contains(getCurrentFlowStateName(variables));
    }
}
