package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<?>> {
    public static final String FLOW_FINAL = "FLOWFINAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    @Resource
    private Map<String, FlowConfiguration<?, ?>> flowConfigurationMap;

    private Map<String, Flow<?, ?>> runningFlows = new ConcurrentHashMap<>();

    @Override
    public void accept(Event<?> event) {
        String key = (String) event.getKey();
        Object payload = event.getData();
        String flowId = getFlowId(event);

        if (!FLOW_FINAL.equals(key)) {
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                FlowConfiguration<?, ?> flowConfig = flowConfigurationMap.get(key);
                flowId = UUID.randomUUID().toString();
                Flow<?, ?> flow = flowConfig.createFlow(flowId);
                runningFlows.put(flowId, flow);
                flow.start();
                flow.sendEvent((String) event.getKey(), payload);
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                Flow<?, ?> flow = runningFlows.get(flowId);
                if (flow != null) {
                    flow.sendEvent(key, payload);
                } else {
                    LOGGER.error("Catch event {} for non existing flow {}", key, flowId);
                }
            }
        } else {
            LOGGER.debug("flow finalizing arrived: id: {}", flowId);
            runningFlows.remove(flowId);
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get("FLOW_ID");
    }
}
