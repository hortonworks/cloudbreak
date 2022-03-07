package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.ContextKeys.PRIVATE_IDS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackScalingService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.flow.core.FlowParameters;

public abstract class AbstractStackDownscaleAction<P extends Payload>
        extends AbstractStackAction<StackDownscaleState, StackDownscaleEvent, StackScalingFlowContext, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackDownscaleAction.class);

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private static final String INSTANCEGROUPNAME = "INSTANCEGROUPNAME";

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private static final String INSTANCEIDS = "INSTANCEIDS";

    // @deprecated haven't removed for compatibility reasons, we should remove it in 2.54
    @Deprecated
    private static final String ADJUSTMENT = "ADJUSTMENT";

    private static final String REPAIR = "REPAIR";

    private static final String HOST_GROUP_WITH_ADJUSTMENT = "HOST_GROUP_WITH_ADJUSTMENT";

    private static final String HOST_GROUP_WITH_PRIVATE_IDS = "HOST_GROUP_WITH_PRIVATE_IDS";

    private static final String HOST_GROUP_WITH_HOSTNAMES = "HOST_GROUP_WITH_HOSTNAMES";

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackScalingService stackScalingService;

    protected AbstractStackDownscaleAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackScalingFlowContext createFlowContext(FlowParameters flowParameters, StateContext<StackDownscaleState, StackDownscaleEvent> stateContext,
            P payload) {
        Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
        LOGGER.info("Variables in AbstractStackDownscaleAction: {}", variables);
        Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
        MDCBuilder.buildMdcContext(stack);
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        CloudContext cloudContext = getCloudContext(stack, location);
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
        CloudStack cloudStack = cloudStackConverter.convert(stack);
        if (payload instanceof StackDownscaleTriggerEvent) {
            return createStackScalingFlowContextFromPayload(flowParameters, (StackDownscaleTriggerEvent) payload, variables, stack, cloudContext,
                    cloudCredential, cloudStack);
        } else {
            return createStackScalingFlowContextFromVariables(flowParameters, variables, stack, cloudContext, cloudCredential, cloudStack);
        }
    }

    private CloudContext getCloudContext(Stack stack, Location location) {
        return CloudContext.Builder.builder()
                .withId(stack.getId())
                .withName(stack.getName())
                .withCrn(stack.getResourceCrn())
                .withPlatform(stack.getCloudPlatform())
                .withVariant(stack.getPlatformVariant())
                .withLocation(location)
                .withWorkspaceId(stack.getWorkspace().getId())
                .withAccountId(stack.getTenant().getId())
                .build();
    }

    private StackScalingFlowContext createStackScalingFlowContextFromPayload(FlowParameters flowParameters, StackDownscaleTriggerEvent payload,
            Map<Object, Object> variables, Stack stack, CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        LOGGER.info("Payload type is StackDownscaleTriggerEvent");
        boolean repair = payload.isRepair();

        Map<String, Set<String>> hostgroupsWithHostnames = payload.getHostGroupsWithHostNames();
        Map<String, Integer> hostGroupsWithAdjustment = payload.getHostGroupsWithAdjustment();
        Map<String, Set<Long>> hostGroupsWithPrivateIds = payload.getHostGroupsWithPrivateIds();
        variables.put(REPAIR, repair);
        variables.put(HOST_GROUP_WITH_ADJUSTMENT, hostGroupsWithAdjustment);
        variables.put(HOST_GROUP_WITH_HOSTNAMES, hostgroupsWithHostnames);
        if (MapUtils.isEmpty(hostGroupsWithPrivateIds) && variables.get(PRIVATE_IDS) != null) {
            hostGroupsWithPrivateIds = getHostGroupsWithPrivateIdsFromVariables(variables, stack);
        }
        if (hostGroupsWithPrivateIds.values().stream().mapToLong(Collection::size).sum() == 0) {
            hostGroupsWithPrivateIds = createHostGroupsWithPrivateIdsFromUnusedPrivateIds(stack, hostGroupsWithAdjustment);
        }
        variables.put(HOST_GROUP_WITH_PRIVATE_IDS, hostGroupsWithPrivateIds);
        LOGGER.info("Variables in AbstractStackDownscaleAction: {}", variables);
        return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack,
                hostGroupsWithAdjustment, hostGroupsWithPrivateIds, hostgroupsWithHostnames, repair,
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null));
    }

    private Map<String, Set<Long>> createHostGroupsWithPrivateIdsFromUnusedPrivateIds(Stack stack, Map<String, Integer> hostGroupsWithAdjustment) {
        Map<String, Set<Long>> hostGroupsWithPrivateIds;
        LOGGER.info("No private ids for hostgroups, lets fill it with unused ones");
        hostGroupsWithPrivateIds = new HashMap<>();
        for (Map.Entry<String, Integer> hostGroupWithAdjustment : hostGroupsWithAdjustment.entrySet()) {
            String hostGroup = hostGroupWithAdjustment.getKey();
            Set<Long> unusedInstanceIds = stackScalingService.getUnusedPrivateIds(hostGroup, hostGroupWithAdjustment.getValue(), stack);
            hostGroupsWithPrivateIds.put(hostGroup, unusedInstanceIds);
        }
        return hostGroupsWithPrivateIds;
    }

    private Map<String, Set<Long>> getHostGroupsWithPrivateIdsFromVariables(Map<Object, Object> variables, Stack stack) {
        Map<String, Set<Long>> hostGroupsWithPrivateIds;
        Set<Long> privateIds = (Set<Long>) variables.get(PRIVATE_IDS);
        LOGGER.info("Private ids filled from variables: {}", privateIds);
        hostGroupsWithPrivateIds = stack.getInstanceGroups().stream().collect(Collectors.toMap(InstanceGroup::getGroupName,
                instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream()
                                    .map(InstanceMetaData::getPrivateId)
                                    .filter(privateIds::contains)
                                    .collect(Collectors.toSet())
                )).entrySet().stream().filter(entry -> entry.getValue().size() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return hostGroupsWithPrivateIds;
    }

    private StackScalingFlowContext createStackScalingFlowContextFromVariables(FlowParameters flowParameters, Map<Object, Object> variables, Stack stack,
            CloudContext cloudContext, CloudCredential cloudCredential, CloudStack cloudStack) {
        Map<String, Integer> hostGroupWithAdjustment = getHostGroupWithAdjustment(variables);
        Map<String, Set<Long>> hostGroupWithPrivateIds = getHostGroupWithPrivateIds(stack.getId(), variables);
        Map<String, Set<String>> hostgroupWithHostnames = getHostGroupWithHostnames(stack.getId(), variables);
        return new StackScalingFlowContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack,
                hostGroupWithAdjustment, hostGroupWithPrivateIds, hostgroupWithHostnames, isRepair(variables),
                new AdjustmentTypeWithThreshold(AdjustmentType.BEST_EFFORT, null));
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackScalingFlowContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
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

    protected Map<String, Set<Long>> getHostGroupWithPrivateIds(Long stackId, Map<Object, Object> variables) {
        if (!variables.containsKey(HOST_GROUP_WITH_PRIVATE_IDS) && variables.containsKey(INSTANCEGROUPNAME) && variables.containsKey(INSTANCEIDS)) {
            Set<String> instanceIds = (Set<String>) variables.get(INSTANCEIDS);
            List<InstanceMetaData> instanceMetaDataList = instanceMetaDataService.findByStackIdAndInstanceIds(stackId, instanceIds);
            Set<Long> privateIds = instanceMetaDataList.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toSet());
            variables.put(HOST_GROUP_WITH_PRIVATE_IDS, Collections.singletonMap(variables.get(INSTANCEGROUPNAME), privateIds));
        }
        return (Map<String, Set<Long>>) variables.get(HOST_GROUP_WITH_PRIVATE_IDS);
    }

    protected Map<String, Set<String>> getHostGroupWithHostnames(Long stackId, Map<Object, Object> variables) {
        if (!variables.containsKey(HOST_GROUP_WITH_HOSTNAMES) && variables.containsKey(INSTANCEGROUPNAME) && variables.containsKey(INSTANCEIDS)) {
            Set<String> instanceIds = (Set<String>) variables.get(INSTANCEIDS);
            List<InstanceMetaData> instanceMetaDataList = instanceMetaDataService.findByStackIdAndInstanceIds(stackId, instanceIds);
            Set<String> fqdns = instanceMetaDataList.stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.toSet());
            variables.put(HOST_GROUP_WITH_HOSTNAMES, Collections.singletonMap(variables.get(INSTANCEGROUPNAME), fqdns));
        }
        return (Map<String, Set<String>>) variables.get(HOST_GROUP_WITH_HOSTNAMES);
    }
}
