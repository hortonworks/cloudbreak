package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackStopFlowTriggerCondition implements FlowTriggerCondition {
    @Inject
    private StackService stackService;

    @Inject
    private StackStartStopService stackStartStopService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stack = stackService.getViewByIdWithoutAuth(stackId);
        return stackStartStopService.isStopPossible(stack);
    }
}
