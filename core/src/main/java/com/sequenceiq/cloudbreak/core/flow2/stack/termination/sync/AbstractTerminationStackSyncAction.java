package com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractTerminationStackSyncAction<P extends Payload>
        extends AbstractStackAction<TerminationStackSyncState, TerminationStackSyncEvent, TerminationStackSyncContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackUtil stackUtil;

    protected AbstractTerminationStackSyncAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected TerminationStackSyncContext createFlowContext(FlowParameters flowParameters,
            StateContext<TerminationStackSyncState, TerminationStackSyncEvent> stateContext, P payload) {
        Long stackId = payload.getResourceId();
        StackView stack = stackDtoService.getStackViewById(stackId);
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspaceId())
                .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                .withTenantId(stack.getTenantId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        List<InstanceMetadataView> allAvailableInstanceMetadata = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stackId);
        return new TerminationStackSyncContext(flowParameters, stack, allAvailableInstanceMetadata, cloudContext, cloudCredential);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<TerminationStackSyncContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
