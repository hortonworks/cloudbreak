package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_TRIGGER_EVENT;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleTriggerEvent;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RollingVerticalScaleFlowEventChainFactory implements FlowEventChainFactory<RollingVerticalScaleFlowChainTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleFlowEventChainFactory.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RollingVerticalScaleFlowChainTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>(getRollingVerticalScalingTriggerEvents(event));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private List<Selectable> getRollingVerticalScalingTriggerEvents(RollingVerticalScaleFlowChainTriggerEvent event) {
        StackDto stackDto = stackDtoService.getById(event.getResourceId());
        String hostGroup = event.getRequest().getGroup();
        List<InstanceGroupDto> instanceGroupDtos = stackDto.getInstanceGroupDtos();
        List<String> instanceIds =  instanceGroupDtos.stream()
                .filter(r -> r.getInstanceGroup().getGroupName().equals(hostGroup))
                .flatMap(r -> r.getInstanceMetadataViews().stream())
                .map(InstanceMetadataView::getInstanceId).toList();

        return Collections.singletonList(new RollingVerticalScaleTriggerEvent(ROLLING_VERTICALSCALE_TRIGGER_EVENT.event(),
                event.getResourceId(), instanceIds, event.getRequest(), event.accepted()));
    }
}
