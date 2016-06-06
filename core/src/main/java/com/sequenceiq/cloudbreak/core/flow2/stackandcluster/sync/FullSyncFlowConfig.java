package com.sequenceiq.cloudbreak.core.flow2.stackandcluster.sync;

import java.util.EnumSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow2.ChainFlow;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;

@Component
public class FullSyncFlowConfig extends StackSyncFlowConfig {
    private static final EnumSet<StackSyncEvent> OWNEVENTS = EnumSet.of(StackSyncEvent.FULL_SYNC_EVENT);

    @Inject
    private StackService stackService;

    @Override
    public Flow createFlow(String flowId) {
        return new ChainFlow(super.createFlow(flowId)) {
            public String nextSelector() {
                return FlowPhases.CLUSTER_SYNC.name();
            }

            @Override
            public Object nextPayload(Event<? extends Payload> event) {
                return new StackEvent(event.getData().getStackId());
            }
        };
    }

    @Override
    public StackSyncEvent[] getEvents() {
        return OWNEVENTS.toArray(new StackSyncEvent[]{});
    }

    @Override
    public StackSyncEvent[] getInitEvents() {
        return new StackSyncEvent[] {
                StackSyncEvent.FULL_SYNC_EVENT
        };
    }
}
