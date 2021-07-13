package com.sequenceiq.flow.core.chain;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.chain.config.FlowChainOperationTypeConfig;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.domain.FlowChainLog;

import reactor.bus.Event;
import reactor.fn.Consumer;

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
        flowStatCache.putByFlowChainId(flowChainId, event.getData().getResourceId(), flowOperationType, false);
        flowChains.triggerNextFlow(flowChainId, flowTriggerUserCrn, Map.of(), flowOperationType);
    }

    public void restoreFlowChain(String flowChainId) {
        Optional<FlowChainLog> chainLog = flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        if (chainLog.isPresent()) {
            String flowChainType = chainLog.get().getFlowChainType();
            Queue<Selectable> queue = (Queue<Selectable>) JsonReader.jsonToJava(chainLog.get().getChain());
            FlowTriggerEventQueue chain = new FlowTriggerEventQueue(flowChainType, queue);
            flowChains.putFlowChain(flowChainId, chainLog.get().getParentFlowChainId(), chain);
            Selectable selectable = queue.peek();
            if (selectable != null) {
                OperationType operationType = flowChainOperationTypeConfig.getFlowTypeOperationTypeMap().getOrDefault(flowChainType, OperationType.UNKNOWN);
                flowStatCache.putByFlowChainId(flowChainId, selectable.getResourceId(), operationType.name(), true);
            }
            if (chainLog.get().getParentFlowChainId() != null) {
                restoreFlowChain(chainLog.get().getParentFlowChainId());
            }
        }
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(Flow2Handler.FLOW_CHAIN_ID);
    }

    private String getFlowTriggerUserCrn(Event<?> event) {
        return event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
    }
}
