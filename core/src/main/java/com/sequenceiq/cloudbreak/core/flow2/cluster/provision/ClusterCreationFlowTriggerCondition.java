package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class ClusterCreationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Long stackId) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.OK;
        StackView stackView = stackService.getViewByIdWithoutAuth(stackId);
        boolean triggerable = stackView.isCreateInProgress();
        if (!triggerable) {
            String msg = "Cluster creation cannot be triggered, because stack is not in create in progress status.";
            LOGGER.warn(msg);
            result = new FlowTriggerConditionResult(msg);
        }
        return result;
    }
}
