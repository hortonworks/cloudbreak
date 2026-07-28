package com.sequenceiq.freeipa.flow.chain;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleChainTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.FreeIpaVerticalScaleService;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.service.freeipa.PrimaryGatewayFirstThenSortByFqdnComparator;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaRollingVerticalScaleFlowEventChainFactory implements FlowEventChainFactory<FreeIpaRollingVerticalScaleChainTriggerEvent>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRollingVerticalScaleFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaVerticalScaleService freeIpaVerticalScaleService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FREEIPA_ROLLING_VERTICAL_SCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(FreeIpaRollingVerticalScaleChainTriggerEvent event) {
        Stack stack = stackService.getByIdWithListsInTransaction(event.getResourceId());
        FreeIpaVerticalScaleParameters scaleConfig = event.getScaleConfig();
        String targetInstanceType = scaleConfig.getTargetInstanceType();
        String originalInstanceType = stack.getInstanceGroups().stream()
                .filter(ig -> ig.getGroupName().equals(scaleConfig.getGroup()))
                .map(ig -> ig.getTemplate().getInstanceType())
                .findFirst().orElse(targetInstanceType);
        FreeIpaVerticalScaleParameters enrichedConfig = scaleConfig.withOriginalInstanceType(originalInstanceType);
        freeIpaVerticalScaleService.updateTemplateWithVerticalScaleInformation(event.getResourceId(), enrichedConfig);

        List<InstanceMetaData> instancesToScale = stack.getNotDeletedInstanceMetaDataSet().stream()
                .sorted(new PrimaryGatewayFirstThenSortByFqdnComparator().reversed())
                .toList();

        LOGGER.info("Rolling vertical scale: {} instances queued for scaling to {}", instancesToScale.size(), targetInstanceType);

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        for (int i = 0; i < instancesToScale.size(); i++) {
            InstanceMetaData instance = instancesToScale.get(i);
            boolean finalInstance = i == instancesToScale.size() - 1;
            flowEventChain.add(new FreeIpaRollingVerticalScaleTriggerEvent(
                    event.getResourceId(),
                    instance.getInstanceId(),
                    enrichedConfig,
                    finalInstance,
                    event.getOperationId()));
        }
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
