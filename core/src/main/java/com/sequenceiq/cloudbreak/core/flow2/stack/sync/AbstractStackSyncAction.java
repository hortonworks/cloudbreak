package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
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

public abstract class AbstractStackSyncAction<P extends Payload> extends AbstractStackAction<StackSyncState, StackSyncEvent, StackSyncContext, P> {
    static final String STATUS_UPDATE_ENABLED = "STATUS_UPDATE_ENABLED";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackUtil stackUtil;

    protected AbstractStackSyncAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackSyncContext createFlowContext(FlowParameters flowParameters, StateContext<StackSyncState, StackSyncEvent> stateContext,
            P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
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
        return new StackSyncContext(flowParameters, stack, allAvailableInstanceMetadata, cloudContext, cloudCredential,
                isStatusUpdateEnabled(variables));
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackSyncContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    private Boolean isStatusUpdateEnabled(Map<Object, Object> variables) {
        return (Boolean) variables.get(STATUS_UPDATE_ENABLED);
    }
}
