package com.sequenceiq.flow.core.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Component
public class FlowChains {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChains.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private FlowLogService flowLogService;

    private final Map<String, FlowTriggerEventQueue> flowChainMap = new ConcurrentHashMap<>();

    private final Map<String, String> flowChainParentMap = new ConcurrentHashMap<>();

    public void putFlowChain(String flowChainId, String parentFlowChainId, FlowTriggerEventQueue flowChain) {
        if (parentFlowChainId != null) {
            flowChainParentMap.put(flowChainId, parentFlowChainId);
            FlowTriggerEventQueue parentFlowChain = flowChainMap.get(parentFlowChainId);
            if (parentFlowChain != null) {
                flowChain = new FlowTriggerEventQueue(parentFlowChain.getFlowChainName() + "/" + flowChain.getFlowChainName(),
                        flowChain.getQueue());
            }
        }
        flowChainMap.put(flowChainId, flowChain);
    }

    public void removeFlowChain(String flowChainId) {
        LOGGER.debug("Remove FlowChain: [{}]", flowChainId);
        if (flowChainId != null) {
            flowChainMap.remove(flowChainId);
        }
    }

    public void removeFullFlowChain(String flowChainId) {
        LOGGER.debug("Remove FullFlowChain: [{}]", flowChainId);
        removeFlowChain(flowChainId);
        String parentFlowChainId;
        while ((parentFlowChainId = flowChainParentMap.remove(flowChainId)) != null) {
            removeFlowChain(parentFlowChainId);
            flowChainId = parentFlowChainId;
        }
    }

    public void removeLastTriggerEvent(String flowChainId, String flowTriggerUserCrn) {
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
        if (flowTriggerEventQueue != null) {
            Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
            if (queue != null) {
                Selectable selectable = queue.poll();
                if (selectable != null) {
                    flowLogService.saveChain(flowChainId, flowChainParentMap.get(flowChainId), flowTriggerEventQueue, flowTriggerUserCrn);
                }
            }
        }
    }

    public void triggerNextFlow(String flowChainId, String flowTriggerUserCrn, Map<Object, Object> contextParams) {
        FlowTriggerEventQueue flowTriggerEventQueue = flowChainMap.get(flowChainId);
        if (flowTriggerEventQueue != null) {
            Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
            if (queue != null) {
                Selectable selectable = queue.peek();
                if (selectable != null) {
                    sendEvent(flowTriggerEventQueue.getFlowChainName(), flowChainId, flowTriggerUserCrn, selectable, contextParams);
                } else {
                    String parentFlowChainId = flowChainParentMap.get(flowChainId);
                    if (parentFlowChainId != null) {
                        flowChainMap.get(parentFlowChainId).getQueue().poll();
                        flowLogService.saveChain(parentFlowChainId, flowChainParentMap.get(parentFlowChainId),
                                flowChainMap.get(parentFlowChainId), flowTriggerUserCrn);
                    }
                    removeFlowChain(flowChainId);
                    triggerParentFlowChain(flowChainId, flowTriggerUserCrn, contextParams);
                }
            }
        }
    }

    protected void sendEvent(String flowChainType, String flowChainId, String flowTriggerUserCrn, Selectable selectable,
            Map<Object, Object> contextParams) {
        LOGGER.debug("Triggering event: {}", selectable);
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_CHAIN_TYPE, flowChainType);
        headers.put(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        headers.put(FlowConstants.FLOW_TRIGGER_USERCRN, flowTriggerUserCrn);
        if (!CollectionUtils.isEmpty(contextParams)) {
            headers.put(FlowConstants.FLOW_CONTEXTPARAMS_ID, contextParams);
        }
        eventBus.notify(selectable.selector(), eventFactory.createEvent(headers, selectable));
    }

    private void triggerParentFlowChain(String flowChainId, String flowTriggerUserCrn, Map<Object, Object> contextParams) {
        String parentFlowChainId = flowChainId != null ? flowChainParentMap.remove(flowChainId) : null;
        if (parentFlowChainId != null) {
            triggerNextFlow(parentFlowChainId, flowTriggerUserCrn, contextParams);
        }
    }
}
