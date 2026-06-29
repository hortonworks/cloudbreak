package com.sequenceiq.freeipa.flow.freeipa.migration.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.chain.AbstractCommonChainAction;
import com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractMultiAzMigrationInitAction<P extends Payload>
        extends AbstractCommonChainAction<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent, StackContext, P>
        implements OperationAwareAction, FlowChainAwareAction {

    @Inject
    private StackService stackService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialService credentialService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    protected AbstractMultiAzMigrationInitAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters,
            StateContext<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        MDCBuilder.buildMdcContext(stack);
        addMdcOperationIdIfPresent(stateContext.getExtendedState().getVariables());
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
        CloudCredential cloudCredential = credentialConverter.convert(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new MultiAzMigrationInitFailedEvent(payload.getResourceId(), ex);
    }
}
