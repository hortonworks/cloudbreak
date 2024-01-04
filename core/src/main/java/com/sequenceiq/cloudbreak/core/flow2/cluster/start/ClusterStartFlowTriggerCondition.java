package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class ClusterStartFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartFlowTriggerCondition.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView clusterView = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        if (clusterView == null || !stack.isStartInProgress()) {
            String msg = String.format("Cluster start cannot be triggered, because cluster %s.",
                    clusterView == null ? "is null" : "not in startRequested status");
            LOGGER.info(msg);
            result = FlowTriggerConditionResult.fail(msg);
        }
        return result;
    }
}
