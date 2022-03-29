package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeFlowEventChainFactory.class);

    private static final int PRIMARY_GW_EVENT_COUNT = 3;

    private static final int PRIMARY_GW_INSTANCE_COUNT = 1;

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

        int nonPrimaryGwInstanceCount = event.getInstanceIds().size();
        int instanceCountForUpscale = nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT + 1;
        int instanceCountForDownscale = nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT;
        flowEventChain.addAll(createScaleEventsForNonPgwInstances(event, instanceCountForUpscale, instanceCountForDownscale));
        flowEventChain.addAll(createScaleEventsAndChangePgw(event, instanceCountForUpscale, instanceCountForDownscale));
        flowEventChain.add(new SaltUpdateTriggerEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted(), true, true)
                .withOperationId(event.getOperationId()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private List<Selectable> createScaleEventsAndChangePgw(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale) {
        LOGGER.debug("Add events for primary gateway with id: [{}]", event.getPrimareGwInstanceId());
        List<Selectable> events = new ArrayList<>(PRIMARY_GW_EVENT_COUNT);
        events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
        ArrayList<String> oldInstances = new ArrayList<>(event.getInstanceIds());
        oldInstances.add(event.getPrimareGwInstanceId());
        events.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                oldInstances, Boolean.FALSE, event.getOperationId()));
        events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), event.getResourceId(), Lists.newArrayList(event.getPrimareGwInstanceId()),
                instanceCountForDownscale, false, true, false, event.getOperationId()));
        return events;
    }

    private List<Selectable> createScaleEventsForNonPgwInstances(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale) {
        LOGGER.debug("Add scale events for non primary gateway instances. upscale count: [{}] downscale count: [{}]",
                instanceCountForUpscale, instanceCountForDownscale);
        List<Selectable> events = new ArrayList<>(event.getInstanceIds().size() * 2);
        for (String instanceId : event.getInstanceIds()) {
            LOGGER.debug("Add upscale and downscale event for [{}]", instanceId);
            events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(),
                    event.getResourceId(), instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId()));
            events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                    event.getResourceId(), Lists.newArrayList(instanceId), instanceCountForDownscale, false, true, false, event.getOperationId()));
        }
        return events;
    }
}
