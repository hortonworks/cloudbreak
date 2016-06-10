package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class InstanceTerminationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        Stack stack = stackService.getById(stackId);
        boolean result = !stack.isDeleteInProgress();
        if (result) {
            LOGGER.info("Couldn't start instance termination flow because the stack has been terminating.");
        }
        return result;
    }
}
