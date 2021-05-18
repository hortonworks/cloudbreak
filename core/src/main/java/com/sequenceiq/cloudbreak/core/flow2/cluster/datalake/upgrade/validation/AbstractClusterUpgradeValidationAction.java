package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractClusterUpgradeValidationAction<P extends Payload>
        extends AbstractAction<ClusterUpgradeValidationState, ClusterUpgradeValidationStateSelectors, StackContext, P> {

    @Inject
    private StackService stackService;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    protected AbstractClusterUpgradeValidationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<ClusterUpgradeValidationState,
            ClusterUpgradeValidationStateSelectors> stateContext, P payload) {
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());

        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withUserId(stack.getCreator().getUserId())
                .withUserName(stack.getCreator().getUserName())
                .withAccountId(stack.getWorkspace().getId())
                .withAccountUUID(stack.getTenant().getName())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = credentialConverter.convert(credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn()));
        CloudStack cloudStack = stackToCloudStackConverter.convert(stack, Collections.emptyList());
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

}