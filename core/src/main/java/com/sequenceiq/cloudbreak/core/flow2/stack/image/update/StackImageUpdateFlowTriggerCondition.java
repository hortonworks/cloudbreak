package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.FlowTriggerCondition;

@Component
public class StackImageUpdateFlowTriggerCondition implements FlowTriggerCondition {
    @Inject
    private StackService stackService;

    @Override
    public boolean isFlowTriggerable(Long stackId) {
        StackView stack = stackService.getViewByIdWithoutAuth(stackId);
        return stack.isAvailable();
    }
}
