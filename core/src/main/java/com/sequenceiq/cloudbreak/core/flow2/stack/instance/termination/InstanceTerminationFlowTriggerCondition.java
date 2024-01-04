package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

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
public class InstanceTerminationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFlowTriggerCondition.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public FlowTriggerConditionResult isFlowTriggerable(Payload payload) {
        FlowTriggerConditionResult result = FlowTriggerConditionResult.ok();
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        if (stack.isDeleteInProgress()) {
            String msg = "Couldn't start instance termination flow because the stack has been terminating.";
            LOGGER.debug(msg);
            result = FlowTriggerConditionResult.fail(msg);
        }
        return result;
    }
}
