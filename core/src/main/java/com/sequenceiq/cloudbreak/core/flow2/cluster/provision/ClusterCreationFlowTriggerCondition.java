package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterCreationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        Stack stack = stackService.getById(stackId);
        boolean result = stack.isAvailable() && stack.getCluster() != null && stack.getCluster().isRequested();
        if (!result) {
            LOGGER.info("Cluster creation cannot be triggered, because cluster is not in requested status or stack is not available.");
        }
        return result;
    }
}
