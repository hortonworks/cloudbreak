package com.sequenceiq.freeipa.flow.stack.termination.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalResourceAttributes;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

abstract class AbstractStackTerminationAction<P extends Payload>
        extends AbstractStackAction<StackTerminationState, StackTerminationEvent, StackTerminationContext, P> {
    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private ResourceService resourceService;

    protected AbstractStackTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackTerminationContext createFlowContext(FlowParameters flowParameters, StateContext<StackTerminationState,
            StackTerminationEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        CloudContext cloudContext = buildContext(stack);
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        List<CloudResource> resources = resourceService.findAllByStackId(stack.getId()).stream()
                .map(r -> resourceConverter.convert(r))
                .filter(Predicate.not(ExternalResourceAttributes::isExternalResource))
                .collect(Collectors.toList());
        return new StackTerminationContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, resources);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackTerminationContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex, ERROR);
    }
}
