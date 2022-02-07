package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.concurrent.ConcurrentLinkedDeque;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.job.StackJobAdapter;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
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
        JobResource jobResource = stackService.getJobResource(event.getResourceId());
        if (jobResource != null) {
            jobService.schedule(new StackJobAdapter(jobResource), repairScheduleDelayInSeconds);
        }
        return new FlowTriggerEventQueue(getName(), event, new ConcurrentLinkedDeque<>());
    }
}
