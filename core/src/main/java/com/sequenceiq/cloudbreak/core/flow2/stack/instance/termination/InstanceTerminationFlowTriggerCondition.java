package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class InstanceTerminationFlowTriggerCondition implements FlowTriggerCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFlowTriggerCondition.class);

    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stack = stackService.getViewByIdWithoutAuth(stackId);
        boolean result = !stack.isDeleteInProgress();
        if (result) {
            LOGGER.debug("Couldn't start instance termination flow because the stack has been terminating.");
        }
        return result;
    }
}
