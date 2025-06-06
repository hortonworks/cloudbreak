package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.FreeIpaFinishCrossRealmTrustState;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustFailureEvent;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public abstract class AbstractFinishCrossRealmTrustAction<P extends Payload>
        extends AbstractStackAction<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent, StackContext, P>
        implements OperationAwareAction {

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackUpdater stackUpdater;

    protected AbstractFinishCrossRealmTrustAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters,
            StateContext<FreeIpaFinishCrossRealmTrustState, FreeIpaFinishCrossRealmTrustFlowEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        CloudContext cloudContext = buildContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FinishCrossRealmTrustFailureEvent(payload.getResourceId(), ex);
    }

    protected StackUpdater stackUpdater() {
        return stackUpdater;
    }
}
