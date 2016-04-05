package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<?>> {
    public static final String FLOW_FINAL = "FLOWFINAL";
    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private Map<String, FlowConfiguration<?, ?>> flowConfigurationMap;

    private Map<String, Flow> runningFlows = new ConcurrentHashMap<>();

    @Inject
    private EventBus eventBus;

    @Override
    public void accept(Event<?> event) {
        String key = (String) event.getKey();
        Object payload = event.getData();
        String flowId = getFlowId(event);

        if (!FLOW_FINAL.equals(key)) {
            FlowConfiguration<?, ?> flowConfig = flowConfigurationMap.get(key);
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                // TODO this is needed because we have two flow implementations in the same time and we want to avoid conflicts
                if (flowConfig != null) {
                    flowId = UUID.randomUUID().toString();
                    Flow flow = flowConfig.createFlow(flowId);
                    runningFlows.put(flowId, flow);
                    flow.initialize();
                    flowLogService.save(flowId, key, payload, flowConfig.getClass(), flow.getCurrentState());
                    flow.sendEvent(key, payload);
                }
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                Flow flow = runningFlows.get(flowId);
                if (flow != null) {
                    flowLogService.save(flowId, key, payload, flowConfig.getClass(), flow.getCurrentState());
                    flow.sendEvent(key, payload);
                } else {
                    LOGGER.error("Catch event {} for non existing flow {}", key, flowId);
                }
            }
        } else {
            LOGGER.debug("flow finalizing arrived: id: {}", flowId);
            flowLogService.close(flowId);
            Flow flow = runningFlows.remove(flowId);
            if (flow instanceof ChainFlow) {
                ChainFlow cf = (ChainFlow) flow;
                sendEvent(cf.nextSelector(), cf.nextPayload(event));
            }
        }
    }

    private void sendEvent(String selector, Object payload) {
        LOGGER.info("Triggering new flow with event: {}", payload);
        eventBus.notify(selector, new Event(payload));
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get("FLOW_ID");
    }
}
