package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.ContextKeys;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractStackDownscaleAction<P extends Payload>
        extends AbstractStackAction<StackDownscaleState, StackDownscaleEvent, StackScalingFlowContext, P> {
    protected static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    protected static final String INSTANCEIDS = "INSTANCEIDS";

    private static final String ADJUSTMENT = "ADJUSTMENT";

    private static final String REPAIR = "REPAIR";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackDownscaleAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackScalingService stackScalingService;

    @Inject
    private StackUtil stackUtil;

    protected AbstractStackDownscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(FlowParameters flowParameters, StateContext<StackDownscaleState, StackDownscaleEvent> stateContext,
            P payload) {
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
        String instanceGroupName = extractInstanceGroupName(payload, variables);
        Set<String> instanceIds = extractInstanceIds(payload, variables, stack);
        Integer adjustment = extractAdjustment(payload, variables);
        boolean repair = extractRepair(payload, variables);
        CloudStack cloudStack = cloudStackConverter.convertForDownscale(stack, instanceIds);
        return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack, instanceGroupName, instanceIds, adjustment,
                repair, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, adjustment.longValue()));
    }

    private boolean extractRepair(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackDownscaleTriggerEvent) {
            StackDownscaleTriggerEvent ssc = (StackDownscaleTriggerEvent) payload;
            boolean repair = ssc.isRepair();
            variables.put(REPAIR, repair);
            return repair;
        }
        return isRepair(variables);
    }

    private Integer extractAdjustment(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackDownscaleTriggerEvent) {
            StackDownscaleTriggerEvent ssc = (StackDownscaleTriggerEvent) payload;
            Integer adjustment = ssc.getPrivateIds() == null ? ssc.getAdjustment() : -ssc.getPrivateIds().size();
            variables.put(ADJUSTMENT, adjustment);
            return adjustment;
        }
        return getAdjustment(variables);
    }

    private Set<String> extractInstanceIds(P payload, Map<Object, Object> variables, Stack stack) {
        if (payload instanceof StackDownscaleTriggerEvent) {
            StackDownscaleTriggerEvent ssc = (StackDownscaleTriggerEvent) payload;
            Set<Long> privateIds = CollectionUtils.isEmpty(ssc.getPrivateIds()) ? (Set<Long>) variables.get(ContextKeys.PRIVATE_IDS) : ssc.getPrivateIds();
            Set<String> instanceIds;
            if (CollectionUtils.isEmpty(privateIds)) {
                LOGGER.info("No private IDs");
                Map<String, String> unusedInstanceIds = stackScalingService.getUnusedInstanceIds(ssc.getInstanceGroup(), ssc.getAdjustment(), stack);
                instanceIds = new HashSet<>(unusedInstanceIds.keySet());
                LOGGER.info("Unused instance IDs: {}", instanceIds);
            } else {
                Set<InstanceMetaData> imds = stack.getInstanceGroupByInstanceGroupName(ssc.getInstanceGroup()).getNotTerminatedInstanceMetaDataSet();
                instanceIds = imds.stream().filter(imd -> privateIds.contains(imd.getPrivateId())).map(InstanceMetaData::getInstanceId)
                        .collect(Collectors.toSet());
                LOGGER.info("Instance IDs for given private IDs: {}", instanceIds);
            }
            variables.put(INSTANCEIDS, instanceIds);
            return instanceIds;
        }
        return getInstanceIds(variables);
    }

    private String extractInstanceGroupName(P payload, Map<Object, Object> variables) {
        if (payload instanceof StackScaleTriggerEvent) {
            StackScaleTriggerEvent ssc = (StackScaleTriggerEvent) payload;
            variables.put(INSTANCEGROUPNAME, ssc.getInstanceGroup());
            return ssc.getInstanceGroup();
        }
        return getInstanceGroupName(variables);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }

    protected String getInstanceGroupName(Map<Object, Object> variables) {
        return (String) variables.get(INSTANCEGROUPNAME);
    }

    protected Set<String> getInstanceIds(Map<Object, Object> variables) {
        return (Set<String>) variables.get(INSTANCEIDS);
    }

    protected Integer getAdjustment(Map<Object, Object> variables) {
        return (Integer) variables.get(ADJUSTMENT);
    }

    protected boolean isRepair(Map<Object, Object> variables) {
        return variables.get(REPAIR) != null && (Boolean) variables.get(REPAIR);
    }
}
