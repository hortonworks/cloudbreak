package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class UpgradeEmbeddedDBPreparationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeEmbeddedDBPreparationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Long stackId) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.OK;
        Stack stack = stackService.getByIdWithTransaction(stackId);
        boolean resourcesIsInTriggerableState = stack.isAvailable() && stack.getCluster() != null;
        if (!resourcesIsInTriggerableState) {
            String msg = "Database Server upgrade could not be triggered, because the cluster's state is not available.";
            LOGGER.info(msg);
            result = new FlowTriggerConditionResult(msg);
        }
        return FlowTriggerConditionResult.OK;
    }
}
