package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractFreeIpaCleanupAction<P extends Payload> extends AbstractStackAction<FreeIpaCleanupState, FreeIpaCleanupEvent, FreeIpaContext, P> {

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

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

    protected FreeIpaClient getFreeIpaClient(Stack stack) throws FreeIpaClientException {
        return freeIpaClientFactory.getFreeIpaClientForStack(stack);
    }

    protected CleanupService getCleanupService() {
        return cleanupService;
    }
}
