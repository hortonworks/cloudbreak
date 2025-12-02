package com.sequenceiq.freeipa.flow.stack.stop.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import com.sequenceiq.freeipa.flow.stack.stop.StackStopContext;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopState;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

public abstract class AbstractStackStopAction<P extends Payload>
        extends AbstractStackAction<StackStopState, StackStopEvent, StackStopContext, P> {
    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    protected AbstractStackStopAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackStopContext createFlowContext(FlowParameters flowParameters, StateContext<StackStopState, StackStopEvent> stateContext,
            P payload) {
        Long stackId = payload.getResourceId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataService.findNotTerminatedForStack(stackId));
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
        return new StackStopContext(flowParameters, stack, instances, cloudContext, cloudCredential);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackStopContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex, ERROR);
    }
}
