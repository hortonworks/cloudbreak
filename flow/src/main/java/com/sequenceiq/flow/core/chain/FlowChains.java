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

    private final Map<String, Queue<Selectable>> flowChainMap = new ConcurrentHashMap<>();

    private final Map<String, String> flowChainParentMap = new ConcurrentHashMap<>();

    public void putFlowChain(String flowChainId, String parentFlowChainId, Queue<Selectable> flowChain) {
        flowChainMap.put(flowChainId, flowChain);
        if (parentFlowChainId != null) {
            flowChainParentMap.put(flowChainId, parentFlowChainId);
        }
    }

    public void removeFlowChain(String flowChainId) {
        if (flowChainId != null) {
            flowChainMap.remove(flowChainId);
        }
    }

    public void removeFullFlowChain(String flowChainId) {
        removeFlowChain(flowChainId);
        String parentFlowChainId;
        while ((parentFlowChainId = flowChainParentMap.remove(flowChainId)) != null) {
            removeFlowChain(parentFlowChainId);
            flowChainId = parentFlowChainId;
        }
    }

    public void triggerNextFlow(String flowChainId, String flowTriggerUserCrn, Map<Object, Object> contextParams) {
        Queue<Selectable> queue = flowChainMap.get(flowChainId);
        if (queue != null) {
            Selectable selectable = queue.poll();
            if (selectable != null) {
                sendEvent(flowChainId, flowTriggerUserCrn, selectable, contextParams);
            } else {
                removeFlowChain(flowChainId);
                triggerParentFlowChain(flowChainId, flowTriggerUserCrn, contextParams);
            }
            flowLogService.saveChain(flowChainId, flowChainParentMap.get(flowChainId), queue, flowTriggerUserCrn);
        }
    }

    protected void sendEvent(String flowChainId, String flowTriggerUserCrn, Selectable selectable, Map<Object, Object> contextParams) {
        LOGGER.debug("Triggering event: {}", selectable);
        Map<String, Object> headers = new HashMap<>();
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
