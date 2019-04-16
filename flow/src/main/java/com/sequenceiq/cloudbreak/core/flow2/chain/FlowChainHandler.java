package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_CHAIN_ID;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cedarsoftware.util.io.JsonReader;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowLogService;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;

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

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        String parentFlowChainId = getFlowChainId(event);
        FlowEventChainFactory<Payload> flowEventChainFactory = flowChainConfigMap.get(key);
        String flowChainId = UUID.randomUUID().toString();
        flowChains.putFlowChain(flowChainId, parentFlowChainId, flowEventChainFactory.createFlowTriggerEventQueue(event.getData()));
        flowChains.triggerNextFlow(flowChainId);
    }

    public void restoreFlowChain(String flowChainId) {
        Optional<FlowChainLog> chainLog = flowLogService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId);
        if (chainLog.isPresent()) {
            Queue<Selectable> chain = (Queue<Selectable>) JsonReader.jsonToJava(chainLog.get().getChain());
            flowChains.putFlowChain(flowChainId, chainLog.get().getParentFlowChainId(), chain);
            if (chainLog.get().getParentFlowChainId() != null) {
                restoreFlowChain(chainLog.get().getParentFlowChainId());
            }
        }
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get(FLOW_CHAIN_ID);
    }
}
