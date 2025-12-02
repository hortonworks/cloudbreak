package com.sequenceiq.freeipa.flow.stack.start.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartContext;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartState;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractStackStartAction<P extends Payload> extends AbstractStackAction<StackStartState, StackStartEvent, StackStartContext, P> {

    @Inject
    private StackService stackService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    protected AbstractStackStartAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackStartContext createFlowContext(FlowParameters flowParameters, StateContext<StackStartState, StackStartEvent> stateContext, P payload) {
        Long stackId = payload.getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        Set<InstanceMetaData> instances = stack.getNotDeletedInstanceMetaDataSet();
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformvariant())
                .withLocation(location)
                .withUserName(stack.getOwner())
                .withAccountId(stack.getAccountId())
                .build();
        Credential credential = credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn());
        CloudCredential cloudCredential = credentialConverter.convert(credential);
        return new StackStartContext(flowParameters, stack, instances, cloudContext, cloudCredential);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackStartContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex, ERROR);
    }
}
