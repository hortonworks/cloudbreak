package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;

@Component
public class UpgradeFlowEventChainFactory implements FlowEventChainFactory<UpgradeEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(), true, false)
                .withOperationId(event.getOperationId()));
        flowEventChain.add(new ImageChangeEvent(IMAGE_CHANGE_EVENT.event(), event.getResourceId(), event.getImageSettingsRequest())
                .withOperationId(event.getOperationId()));

        int instanceCountForUpscale = event.getInstanceIds().size() + 2;
        int instanceCountForDownscale = event.getInstanceIds().size() + 1;
        addScaleEventsForNonPgwInstances(event, flowEventChain, instanceCountForUpscale, instanceCountForDownscale);
        addScaleEventsAndChangePgw(event, flowEventChain, instanceCountForUpscale, instanceCountForDownscale);
        flowEventChain.add(new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(), true, true)
                .withOperationId(event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), flowEventChain);
    }

    private void addScaleEventsAndChangePgw(UpgradeEvent event, Queue<Selectable> flowEventChain, int instanceCountForUpscale, int instanceCountForDownscale) {
        flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
        List<String> oldInstances = new ArrayList<>(event.getInstanceIds());
        oldInstances.add(event.getPrimareGwInstanceId());
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                oldInstances, Boolean.FALSE, event.getOperationId()));
        flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), List.of(event.getPrimareGwInstanceId()), instanceCountForDownscale, false, true, false, event.getOperationId()));
    }

    private void addScaleEventsForNonPgwInstances(UpgradeEvent event, Queue<Selectable> flowEventChain, int instanceCountForUpscale,
            int instanceCountForDownscale) {
        for (String instanceId : event.getInstanceIds()) {
            flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                    event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
            flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                    event.getResourceId(), List.of(instanceId), instanceCountForDownscale, false, true, false, event.getOperationId()));
        }
    }

    @Override
    public String getName() {
        return "FreeIPA upgrade flow";
    }
}
