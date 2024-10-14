package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.FreeIpaProviderTemplateUpdateFlowEvent.FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.FreeIpaProviderTemplateUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate.event.RootVolumeUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;

@Component
public class RootVolumeUpdateFlowEventChainFactory implements FlowEventChainFactory<RootVolumeUpdateEvent> {

    private static final int MAX_NODE_COUNT_FOR_UPSCALE = AvailabilityType.HA.getInstanceCount() + 1;

    private static final int MAX_NODE_COUNT_FOR_DOWNSCALE = AvailabilityType.HA.getInstanceCount();

    @Override
    public String initEvent() {
        return FlowChainTriggers.ROOT_VOLUME_UPDATE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RootVolumeUpdateEvent event) {

        int instanceCountForUpscale = Math.min(event.getInstanceCountByGroup() + 1, MAX_NODE_COUNT_FOR_UPSCALE);
        int instanceCountForDownscale = Math.min(event.getInstanceCountByGroup(), MAX_NODE_COUNT_FOR_DOWNSCALE);
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new FreeIpaProviderTemplateUpdateEvent(FREEIPA_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT.event(), event.getOperationId(),
                event.getResourceId()));
        for (String instanceId : event.getUpdateInstanceIds()) {
            if (!event.getPgwInstanceId().equals(instanceId)) {
                List<String> toBeRemovedInstanceIds = Lists.newArrayList(instanceId);
                addUpscaleEventToQueue(flowEventChain, event, instanceCountForUpscale);
                addDownscaleEventToQueue(flowEventChain, event, toBeRemovedInstanceIds, false, instanceCountForDownscale);
            }
        }
        List<String> primaryGatewayInstanceIds = Lists.newArrayList(event.getPgwInstanceId());
        addUpscaleEventToQueue(flowEventChain, event, instanceCountForUpscale);
        addChangeGatewayEventToQueue(flowEventChain, event, primaryGatewayInstanceIds, false);
        addDownscaleEventToQueue(flowEventChain, event, primaryGatewayInstanceIds, true, instanceCountForDownscale);
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private void addDownscaleEventToQueue(Queue<Selectable> flowEventChain, RootVolumeUpdateEvent event, List<String> terminatedOrRemovedInstanceIds,
            boolean finalChain, int instanceCountForDownscale) {
        flowEventChain.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                event.getResourceId(), terminatedOrRemovedInstanceIds, instanceCountForDownscale, true, true, finalChain, event.getOperationId()));
    }

    private void addUpscaleEventToQueue(Queue<Selectable> flowEventChain, RootVolumeUpdateEvent event, int instanceCountForUpscale) {
        flowEventChain.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), new ArrayList<>(), instanceCountForUpscale, true, true, false, event.getOperationId(), null));
    }

    private void addChangeGatewayEventToQueue(Queue<Selectable> flowEventChain, RootVolumeUpdateEvent event, List<String> primaryGatewayInstanceIds,
            boolean finalChain) {
        flowEventChain.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                primaryGatewayInstanceIds, finalChain, event.getOperationId(), event.accepted()));
    }
}
