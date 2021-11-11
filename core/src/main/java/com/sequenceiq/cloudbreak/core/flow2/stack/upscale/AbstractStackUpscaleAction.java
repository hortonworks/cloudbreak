package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackScalingFlowContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.flow.core.FlowParameters;

abstract class AbstractStackUpscaleAction<P extends Payload> extends AbstractStackAction<StackUpscaleState, StackUpscaleEvent, StackScalingFlowContext, P> {
    static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    static final String ADJUSTMENT = "ADJUSTMENT";

    static final String UPSCALE_CANDIDATE_ADDRESSES = "UPSCALE_CANDIDATE_ADDRESSES";

    static final String HOSTNAMES = "HOSTNAMES";

    static final String REPAIR = "REPAIR";

    static final String NETWORK_SCALE_DETAILS = "NETWORK_SCALE_DETAILS";

    static final String ADJUSTMENT_WITH_THRESHOLD = "ADJUSTMENT_WITH_THRESHOLD";

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackUtil stackUtil;

    AbstractStackUpscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(FlowParameters flowParameters,
            StateContext<StackUpscaleState, StackUpscaleEvent> stateContext, P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, getInstanceGroupName(variables),
                Collections.emptySet(), getAdjustment(variables), getHostNames(variables), isRepair(variables), getStackNetworkScaleDetails(variables),
                getAdjustmentWithThreshold(variables));
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    private String getInstanceGroupName(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCEGROUPNAME);
    }

    private Integer getAdjustment(Map<Object, Object> variables) {
        return (Integer) variables.get(ADJUSTMENT);
    }

    private boolean isRepair(Map<Object, Object> variables) {
        return variables.get(REPAIR) != null && (Boolean) variables.get(REPAIR);
    }

    private Set<String> getHostNames(Map<Object, Object> variables) {
        return (Set<String>) variables.get(HOSTNAMES);
    }

    private NetworkScaleDetails getStackNetworkScaleDetails(Map<Object, Object> variables) {
        return (NetworkScaleDetails) variables.get(NETWORK_SCALE_DETAILS);
    }

    private AdjustmentTypeWithThreshold getAdjustmentWithThreshold(Map<Object, Object> variables) {
        return (AdjustmentTypeWithThreshold) variables.get(ADJUSTMENT_WITH_THRESHOLD);
    }

}
