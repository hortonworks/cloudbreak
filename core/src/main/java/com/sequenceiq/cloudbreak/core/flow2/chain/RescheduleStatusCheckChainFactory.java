package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.concurrent.ConcurrentLinkedDeque;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.job.StackJobAdapter;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RescheduleStatusCheckChainFactory implements FlowEventChainFactory<RescheduleStatusCheckTriggerEvent> {

    @Inject
    private StackService stackService;

    @Inject
    private StatusCheckerJobService jobService;

    @Value("${cb.repair.schedule.delay:180}")
    private int repairScheduleDelayInSeconds;

    @Override
    public String initEvent() {
        return FlowChainTriggers.RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RescheduleStatusCheckTriggerEvent event) {
        StackView stack = stackService.getViewByIdWithoutAuth(event.getResourceId());
        if (stack != null && stack.isAvailable() && stack.getClusterView() != null) {
            jobService.schedule(new StackJobAdapter(convertToStack(stack)), repairScheduleDelayInSeconds);
        }
        return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedDeque<>());
    }

    private Stack convertToStack(StackView view) {
        Stack result = new Stack();
        result.setId(view.getId());
        result.setResourceCrn(view.getResourceCrn());
        return result;
    }
}
