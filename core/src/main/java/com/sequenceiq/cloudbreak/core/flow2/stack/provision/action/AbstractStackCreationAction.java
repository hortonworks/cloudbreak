package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public abstract class AbstractStackCreationAction<P extends Payload> extends AbstractAction<StackCreationState, StackCreationEvent, StackContext, P> {
    @Inject
    private StackService stackService;
    @Inject
    private StackToCloudStackConverter cloudStackConverter;
    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    protected AbstractStackCreationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected StackContext createFlowContext(String flowId, StateContext<StackCreationState, StackCreationEvent> stateContext, P payload) {
        Stack stack = stackService.getById(payload.getStackId());
        // TODO LogAspect!!
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                location);
        CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowId, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FlowFailureEvent(payload.getStackId(), ex);
    }
}
