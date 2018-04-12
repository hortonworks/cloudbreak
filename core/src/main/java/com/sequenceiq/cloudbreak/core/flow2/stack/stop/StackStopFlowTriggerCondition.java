package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class StackStopFlowTriggerCondition implements FlowTriggerCondition {
    @Inject
    private StackService stackService;

    @Inject
    private StackStartStopService stackStartStopService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stack = stackService.getByIdView(stackId);
        return stackStartStopService.isStopPossible(stack);
    }
}
