package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class StackImageUpdateFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageUpdateFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Long stackId) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.OK;
        StackView stack = stackService.getViewByIdWithoutAuth(stackId);
        Status clusterStatus = stack.getClusterView().getStatus();
        boolean triggerable = stack.isAvailable() && clusterStatus.isAvailable();
        if (!triggerable) {
            String msg = "Image update cannot be triggered because the cluster is not available.";
            LOGGER.debug(msg);
            result = new FlowTriggerConditionResult(msg);
        }
        return result;
    }
}
