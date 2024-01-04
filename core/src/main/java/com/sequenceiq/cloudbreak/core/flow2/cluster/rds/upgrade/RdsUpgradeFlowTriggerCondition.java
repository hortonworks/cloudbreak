package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class RdsUpgradeFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeFlowTriggerCondition.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        boolean resourcesAreInTriggerableState = stack.isAvailable() && stack.getClusterId() != null;
        if (!resourcesAreInTriggerableState) {
            String msg = "Database Server upgrade could not be triggered, because the cluster's state is not available.";
            LOGGER.info(msg);
            result = FlowTriggerConditionResult.fail(msg);
        }
        return result;
    }
}
