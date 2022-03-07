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

import org.apache.commons.lang3.StringUtils;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
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

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    static final String ADJUSTMENT = "ADJUSTMENT";

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    static final String HOSTNAMES = "HOSTNAMES";

    static final String UPSCALE_CANDIDATE_ADDRESSES = "UPSCALE_CANDIDATE_ADDRESSES";

    static final String HOST_GROUP_WITH_ADJUSTMENT = "HOST_GROUP_WITH_ADJUSTMENT";

    static final String HOST_GROUP_WITH_HOSTNAMES = "HOST_GROUP_WITH_HOSTNAMES";

    static final String REPAIR = "REPAIR";

    static final String TRIGGERED_VARIANT = "TRIGGERED_VARIANT";

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
                .withVariant(getTriggeredVariantOrStackVariant(variables, stack))
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        if (payload instanceof StackScaleTriggerEvent) {
            StackScaleTriggerEvent stackScaleTriggerEvent = (StackScaleTriggerEvent) payload;
            boolean repair = stackScaleTriggerEvent.isRepair();
            Map<String, Set<String>> hostgroupsWithHostnames = stackScaleTriggerEvent.getHostGroupsWithHostNames();
            Map<String, Integer> hostGroupsWithAdjustment = stackScaleTriggerEvent.getHostGroupsWithAdjustment();
            Map<String, Set<Long>> hostGroupsWithPrivateIds = stackScaleTriggerEvent.getHostGroupsWithPrivateIds();
            NetworkScaleDetails networkScaleDetails = stackScaleTriggerEvent.getNetworkScaleDetails();
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = stackScaleTriggerEvent.getAdjustmentTypeWithThreshold();
            variables.put(REPAIR, repair);
            variables.put(HOST_GROUP_WITH_ADJUSTMENT, hostGroupsWithAdjustment);
            variables.put(HOST_GROUP_WITH_HOSTNAMES, hostgroupsWithHostnames);
            variables.put(NETWORK_SCALE_DETAILS, networkScaleDetails);
            variables.put(ADJUSTMENT_WITH_THRESHOLD, adjustmentTypeWithThreshold);
            return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, hostGroupsWithAdjustment,
                    hostGroupsWithPrivateIds, hostgroupsWithHostnames, repair, networkScaleDetails, adjustmentTypeWithThreshold);
        } else {
            Map<String, Integer> hostGroupWithAdjustment = getHostGroupWithAdjustment(variables);
            Map<String, Set<String>> hostgroupWithHostnames = getHostGroupWithHostnames(variables);
            NetworkScaleDetails stackNetworkScaleDetails = getStackNetworkScaleDetails(variables);
            AdjustmentTypeWithThreshold adjustmentWithThreshold = getAdjustmentWithThreshold(variables);
            return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack,
                    hostGroupWithAdjustment, null, hostgroupWithHostnames, isRepair(variables), stackNetworkScaleDetails,
                    adjustmentWithThreshold);
        }
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    private String getTriggeredVariantOrStackVariant(Map<Object, Object> variables, Stack stack) {
        String variant = (String) variables.get(TRIGGERED_VARIANT);
        if (StringUtils.isEmpty(variant)) {
            variant = stack.getPlatformVariant();
        }
        return variant;
    }

    protected boolean isRepair(Map<Object, Object> variables) {
        return variables.get(REPAIR) != null && (Boolean) variables.get(REPAIR);
    }

    protected Map<String, Integer> getHostGroupWithAdjustment(Map<Object, Object> variables) {
        if (!variables.containsKey(HOST_GROUP_WITH_ADJUSTMENT) && variables.containsKey(INSTANCEGROUPNAME) && variables.containsKey(ADJUSTMENT)) {
            variables.put(HOST_GROUP_WITH_ADJUSTMENT, Collections.singletonMap(variables.get(INSTANCEGROUPNAME), variables.get(ADJUSTMENT)));
        }
        return (Map<String, Integer>) variables.get(HOST_GROUP_WITH_ADJUSTMENT);
    }

    protected Map<String, Set<String>> getHostGroupWithHostnames(Map<Object, Object> variables) {
        if (!variables.containsKey(HOST_GROUP_WITH_HOSTNAMES) && variables.containsKey(INSTANCEGROUPNAME) && variables.containsKey(HOSTNAMES)) {
            variables.put(HOST_GROUP_WITH_HOSTNAMES, Collections.singletonMap(variables.get(INSTANCEGROUPNAME), variables.get(HOSTNAMES)));
        }
        return (Map<String, Set<String>>) variables.get(HOST_GROUP_WITH_HOSTNAMES);
    }

    private NetworkScaleDetails getStackNetworkScaleDetails(Map<Object, Object> variables) {
        return (NetworkScaleDetails) variables.get(NETWORK_SCALE_DETAILS);
    }

    private AdjustmentTypeWithThreshold getAdjustmentWithThreshold(Map<Object, Object> variables) {
        return (AdjustmentTypeWithThreshold) variables.get(ADJUSTMENT_WITH_THRESHOLD);
    }

}
