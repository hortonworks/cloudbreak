package com.sequenceiq.cloudbreak.core.flow2.stackandcluster.stop;

import java.util.EnumSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.ChainFlow;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopFlowConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;

@Component
public class ClusterAndStackStopFlowConfig extends ClusterStopFlowConfig {
    private static final EnumSet<ClusterStopEvent> OWNEVENTS = EnumSet.of(ClusterStopEvent.CLUSTER_AND_STACK_STOP_EVENT);

    @Inject
    private StackService stackService;


    @Override
    public Flow createFlow(String flowId) {
        return new ChainFlow(super.createFlow(flowId)) {
            @Override
            public String nextSelector() {
                return FlowPhases.STACK_STOP.name();
            }

            @Override
            public Object nextPayload(Event<? extends Payload> event) {
                Payload p = event.getData();
                Stack stack = stackService.getById(p.getStackId());
                return new StackStatusUpdateContext(p.getStackId(), Platform.platform(stack.cloudPlatform()), false);
            }
        };
    }

    @Override
    public ClusterStopEvent[] getEvents() {
        return OWNEVENTS.toArray(new ClusterStopEvent[]{});
    }

    @Override
    public ClusterStopEvent[] getInitEvents() {
        return new ClusterStopEvent[] {
                ClusterStopEvent.CLUSTER_AND_STACK_STOP_EVENT
        };
    }
}
