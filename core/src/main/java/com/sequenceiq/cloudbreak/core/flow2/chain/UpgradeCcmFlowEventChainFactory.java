package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.ccm.upgrade.UpgradeCcmEvent.UPGRADE_CCM_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm.UpgradeCcmTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class UpgradeCcmFlowEventChainFactory implements FlowEventChainFactory<UpgradeCcmFlowChainTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_CCM_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeCcmFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(
                new UpgradeCcmTriggerRequest(UPGRADE_CCM_EVENT.event(), event.getResourceId(), event.getClusterId(), event.getOldTunnel(), null));
        flowEventChain.add(UserDataUpdateRequest.builder()
                .withSelector(UPDATE_USERDATA_TRIGGER_EVENT.event())
                .withStackId(event.getResourceId())
                .withOldTunnel(event.getOldTunnel())
                .build());
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
