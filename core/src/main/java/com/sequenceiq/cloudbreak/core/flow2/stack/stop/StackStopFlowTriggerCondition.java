package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;

@Component
public class StackStopFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackStartStopService stackStartStopService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        StackView stack = stackService.getViewByIdWithoutAuth(payload.getResourceId());
        if (!stackStartStopService.isStopPossible(stack)) {
            String msg = "Stopping stack is not possible because stack is not in stop requested state.";
            LOGGER.debug(msg);
            result = FlowTriggerConditionResult.fail(msg);
        }
        return result;
    }
}
