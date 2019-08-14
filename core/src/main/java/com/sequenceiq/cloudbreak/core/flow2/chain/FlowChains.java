package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_CHAIN_ID;
import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_CONTEXTPARAMS_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

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

    public void triggerNextFlow(String flowChainId, Map<Object, Object> contextParams) {
        Queue<Selectable> queue = flowChainMap.get(flowChainId);
        if (queue != null) {
            Selectable selectable = queue.poll();
            if (selectable != null) {
                sendEvent(flowChainId, selectable, contextParams);
            } else {
                removeFlowChain(flowChainId);
                triggerParentFlowChain(flowChainId, contextParams);
            }
            flowLogService.saveChain(flowChainId, flowChainParentMap.get(flowChainId), queue);
        }
    }

    protected void sendEvent(String flowChainId, Selectable selectable, Map<Object, Object> contextParams) {
        LOGGER.info("Triggering event: {}", selectable);
        Map<String, Object> headers = new HashMap<>();
        headers.put(FLOW_CHAIN_ID, flowChainId);
        if (!CollectionUtils.isEmpty(contextParams)) {
            headers.put(FLOW_CONTEXTPARAMS_ID, contextParams);
        }
        eventBus.notify(selectable.selector(), eventFactory.createEvent(headers, selectable));
    }

    private void triggerParentFlowChain(String flowChainId, Map<Object, Object> contextParams) {
        String parentFlowChainId = flowChainId != null ? flowChainParentMap.remove(flowChainId) : null;
        if (parentFlowChainId != null) {
            triggerNextFlow(parentFlowChainId, contextParams);
        }
    }
}
