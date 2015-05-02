package com.sequenceiq.cloudbreak.core.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.LiveNodeCounterContext;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class LiveNodeCheckerTask extends StackBasedStatusCheckerTask<LiveNodeCounterContext> {

    @Override
    public boolean checkStatus(LiveNodeCounterContext liveNodeCounterContext) {
        ContainerOrchestratorCluster cluster = liveNodeCounterContext.getCluster();
        return liveNodeCounterContext.getContainerOrchestrator().areAllNodesAvailable(cluster.getApiAddress(), cluster.getNodes());
    }

    @Override
    public void handleTimeout(LiveNodeCounterContext t) {
        throw new InternalServerException("Operation timed out. Swarm manager couldn't start or the agents didn't join in time.");
    }

    @Override
    public String successMessage(LiveNodeCounterContext t) {
        return String.format("Swarm is available and the agents are registered.");
    }
}
