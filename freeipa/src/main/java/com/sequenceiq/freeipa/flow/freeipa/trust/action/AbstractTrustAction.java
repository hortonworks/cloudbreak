package com.sequenceiq.freeipa.flow.freeipa.trust.action;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.TrustStatus;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

public abstract class AbstractTrustAction<S extends FlowState, E extends FlowEvent, P extends Payload>
        extends AbstractStackAction<S, E, StackContext, P>
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

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    protected AbstractTrustAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<S, E> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        CloudContext cloudContext = buildContext(stack);
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    protected void updateStatuses(Stack stack, DetailedStackStatus detailedStackStatus, String statusReason, TrustStatus trustStatus) {
        stackUpdater.updateStackStatus(stack, detailedStackStatus, statusReason);
        crossRealmTrustService.updateTrustStateByStackId(stack.getId(), trustStatus);
    }

    protected void setOperationId(Stack stack, Map<Object, Object> variables, String operationId) {
        setOperationId(variables, operationId);
        crossRealmTrustService.updateOperationIdByStackId(stack.getId(), operationId);
    }
}
