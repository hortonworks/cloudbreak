package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClustersUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class EphemeralFlowEventChainFactory implements FlowEventChainFactory<EphemeralClustersUpgradeTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EphemeralFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private ReactorFlowManager flowManager;

    @Override
    public String initEvent() {
        return FlowChainTriggers.EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(EphemeralClustersUpgradeTriggerEvent event) {
        Set<StackIdView> ephemeralStacks = stackService.findClustersConnectedToDatalake(event.getResourceId());
        for (StackIdView stack : ephemeralStacks) {
            try {
                flowManager.triggerEphemeralUpdate(stack.getId());
            } catch (Exception e) {
                LOGGER.debug("Cannot trigger ephemeral cluster update for stack: " + stack.getName(), e);
            }
        }
        return new FlowTriggerEventQueue(getName(), new ConcurrentLinkedDeque<>());
    }
}
