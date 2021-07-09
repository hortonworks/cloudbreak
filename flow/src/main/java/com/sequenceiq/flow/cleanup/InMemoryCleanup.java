package com.sequenceiq.flow.cleanup;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryResourceStateStore;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.cache.FlowStatCache;

@Component
public class InMemoryCleanup {

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowStatCache flowStatCache;

    public void cancelEveryFlowWithoutDbUpdate() {
        for (String resourceType : InMemoryResourceStateStore.getResourceTypes()) {
            for (Long resourceId : InMemoryResourceStateStore.getAllResourceId(resourceType)) {
                InMemoryResourceStateStore.putResource(resourceType, resourceId, PollGroup.CANCELLED);
            }
        }
        for (String id : runningFlows.getRunningFlowIds()) {
            cancelFlowWithoutDbUpdate(id);
        }
    }

    public void cancelFlowWithoutDbUpdate(String flowId) {
        String flowChainId = runningFlows.getFlowChainId(flowId);
        if (flowChainId != null) {
            flowChains.removeFullFlowChain(flowChainId, false);
        }
        runningFlows.remove(flowId);
        flowStatCache.remove(flowId, false);
    }

}
