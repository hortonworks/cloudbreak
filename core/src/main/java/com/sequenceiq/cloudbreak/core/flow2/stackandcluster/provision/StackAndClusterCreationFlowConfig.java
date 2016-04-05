package com.sequenceiq.cloudbreak.core.flow2.stackandcluster.provision;

import java.util.EnumSet;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow2.ChainFlow;
import com.sequenceiq.cloudbreak.core.flow2.Flow;
import com.sequenceiq.cloudbreak.core.flow2.FlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationFlowConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;

@Component
public class StackAndClusterCreationFlowConfig extends StackCreationFlowConfig {
    private static final EnumSet<StackCreationEvent> OWNEVENTS = EnumSet.of(StackCreationEvent.START_STACKANDCLUSTER_CREATION_EVENT);

    @Inject
    private StackService stackService;

    @Override
    public Flow createFlow(String flowId) {
        return new ChainFlow(super.createFlow(flowId)) {
            public String nextSelector() {
                return FlowPhases.BOOTSTRAP_CLUSTER.name();
            }

            public Object nextPayload(Event<?> event) {
                Long stackId = ((SelectableFlowStackEvent) event.getData()).getStackId();
                Stack stack = stackService.getById(stackId);
                return new ProvisioningContext.Builder().setDefaultParams(stack.getId(), Platform.platform(stack.cloudPlatform())).build();
            }
        };
    }

    @Override
    public FlowEvent[] getEvents() {
        return OWNEVENTS.toArray(new FlowEvent[]{});
    }
}
