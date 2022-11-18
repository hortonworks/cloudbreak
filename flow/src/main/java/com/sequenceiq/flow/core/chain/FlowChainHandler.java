package com.sequenceiq.flow.core.chain;

import static com.sequenceiq.cloudbreak.service.flowlog.FlowLogUtil.tryDeserializeTriggerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.config.FlowChainOperationTypeConfig;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.domain.FlowChainLog;

@Component
public class FlowChainHandler implements Consumer<Event<? extends Payload>> {

    @Resource
    private Map<String, FlowEventChainFactory<Payload>> flowChainConfigMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowStatCache flowStatCache;

    @Inject
    private FlowChainOperationTypeConfig flowChainOperationTypeConfig;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        String parentFlowChainId = getFlowChainId(event);
        String flowTriggerUserCrn = getFlowTriggerUserCrn(event);
        FlowEventChainFactory<Payload> flowEventChainFactory = flowChainConfigMap.get(key);
        String flowOperationType = flowEventChainFactory.getFlowOperationType().name();
        String flowChainId = UUID.randomUUID().toString();
        flowChains.putFlowChain(flowChainId, parentFlowChainId, flowEventChainFactory.createFlowTriggerEventQueue(event.getData()));
        flowChains.addNotSavedFlowChainLog(flowChainId);
        flowStatCache.putByFlowChainId(flowChainId, event.getData().getResourceId(), flowOperationType, false);
        flowChains.triggerNextFlow(flowChainId, flowTriggerUserCrn, new HashMap<>(), flowOperationType, Optional.empty());
    }

    public void restoreFlowChain(String flowChainId) {
        Optional<FlowChainLog> chainLogOpt = flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        if (chainLogOpt.isPresent()) {
            FlowChainLog chainLog = chainLogOpt.get();
            String flowChainType = chainLog.getFlowChainType();
            Queue<Selectable> queue = chainLog.getChainAsQueue();
            Payload triggerEvent = tryDeserializeTriggerEvent(chainLog);
            FlowTriggerEventQueue chain = new FlowTriggerEventQueue(flowChainType, triggerEvent, queue);
            if (chainLog.getParentFlowChainId() != null) {
                chain.setParentFlowChainId(chainLog.getParentFlowChainId());
            }
            flowChains.putFlowChain(flowChainId, chainLog.getParentFlowChainId(), chain);
            Selectable selectable = queue.peek();
            if (selectable != null) {
                OperationType operationType = flowChainOperationTypeConfig.getFlowTypeOperationTypeMap().getOrDefault(flowChainType, OperationType.UNKNOWN);
                flowStatCache.putByFlowChainId(flowChainId, selectable.getResourceId(), operationType.name(), true);
            }
            if (chainLog.getParentFlowChainId() != null) {
                restoreFlowChain(chainLog.getParentFlowChainId());
            }
        }
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_CHAIN_ID);
    }

    private String getFlowTriggerUserCrn(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
    }
}
