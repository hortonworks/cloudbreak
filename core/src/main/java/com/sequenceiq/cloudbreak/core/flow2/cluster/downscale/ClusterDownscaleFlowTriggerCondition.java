package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class ClusterDownscaleFlowTriggerCondition implements FlowTriggerCondition {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        try {
            ClusterDownscaleTriggerEvent triggerEvent = (ClusterDownscaleTriggerEvent) payload;
            if (triggerEvent.getDetails() != null && triggerEvent.getDetails().isPurgeZombies()) {
                Stack stack = stackService.getByIdWithListsInTransaction(triggerEvent.getResourceId());
                Set<String> zombieHostGroups = triggerEvent.getZombieHostGroups();
                Set<Long> zombieInstances = stack.getZombieInstanceMetaDataSet().stream()
                        .filter(instanceMetaData -> zombieHostGroups.contains(instanceMetaData.getInstanceGroupName()))
                        .map(instanceMetaData -> instanceMetaData.getPrivateId())
                        .collect(Collectors.toSet());
                if (zombieInstances.isEmpty()) {
                    result = FlowTriggerConditionResult.skip("Skip cluster downscale flow, no zombie nodes found.");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cluster downscale trigger condition failed", e);
            result = FlowTriggerConditionResult.fail("Cluster downscale trigger condition failed.");
        }
        return result;
    }
}
