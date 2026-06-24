package com.sequenceiq.redbeams.flow.redbeams.upgrade.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;

@Component
public class RedbeamsUpgradeFlowEventChainFactory implements FlowEventChainFactory<RedbeamsUpgradeFlowChainTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return EventSelectorUtil.selector(RedbeamsUpgradeFlowChainTriggerEvent.class);
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RedbeamsUpgradeFlowChainTriggerEvent event) {
        LOGGER.debug("Creating flow trigger event queue for database server upgrade (validate -> cleanup -> upgrade) with event {}", event);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new RedbeamsStartValidateUpgradeRequest(event.getResourceId(), event.getTargetMajorVersion(),
                event.getMigrationParams(), event.accepted()));
        flowEventChain.add(new RedbeamsStartValidateUpgradeCleanupRequest(event.getResourceId()));
        flowEventChain.add(new RedbeamsStartUpgradeRequest(event.getResourceId(), event.getTargetMajorVersion(), event.getMigrationParams()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
