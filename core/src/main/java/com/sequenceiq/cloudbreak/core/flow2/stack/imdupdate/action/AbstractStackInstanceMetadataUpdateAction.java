package com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.StackInstanceMetadataUpdateState;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.imdupdate.event.StackInstanceMetadataUpdateFailureEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractStackInstanceMetadataUpdateAction<P extends Payload>
        extends AbstractAction<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent, StackContext, P> {

    @Inject
    private StackService stackService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialClientService credentialClientService;

    protected AbstractStackInstanceMetadataUpdateAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters,
            StateContext<StackInstanceMetadataUpdateState, StackInstanceMetadataUpdateEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withUserName(stack.getCreator().getUserName())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new StackInstanceMetadataUpdateFailureEvent(payload.getResourceId(), ex);
    }
}
