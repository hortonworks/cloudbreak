package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.repository.FlowChainLogRepository;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class FlowChainHandler implements Consumer<Event<? extends Payload>> {
    @Resource
    private Map<String, FlowEventChainFactory> flowChainConfigMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowChainLogRepository flowChainLogRepository;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        String parentFlowChainId = getFlowChainId(event);
        FlowEventChainFactory flowEventChainFactory = flowChainConfigMap.get(key);
        String flowChainId = UUID.randomUUID().toString();
        flowChains.putFlowChain(flowChainId, parentFlowChainId, flowEventChainFactory.createFlowTriggerEventQueue(event.getData()));
        flowChains.triggerNextFlow(flowChainId);
    }

    public void restoreFlowChain(String flowChainId) {
        FlowChainLog chainLog = flowChainLogRepository.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        if (chainLog != null) {
            Queue<Selectable> chain = (Queue<Selectable>) JsonReader.jsonToJava(chainLog.getChain());
            flowChains.putFlowChain(flowChainId, chainLog.getParentFlowChainId(), chain);
            if (chainLog.getParentFlowChainId() != null) {
                restoreFlowChain(chainLog.getParentFlowChainId());
            }
        }
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get("FLOW_CHAIN_ID");
    }
}
