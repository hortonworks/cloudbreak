package com.sequenceiq.cloudbreak.core.flow2.restart;

import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_CHAIN_ID;
import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_ID;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.service.ErrorHandlerAwareFlowEventFactory;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component("DefaultRestartAction")
public class DefaultRestartAction implements RestartAction {

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareFlowEventFactory eventFactory;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(FLOW_ID, flowId);
        if (flowChainId != null) {
            headers.put(FLOW_CHAIN_ID, flowChainId);
        }
        eventBus.notify(event, eventFactory.createEvent(new Event.Headers(headers), payload));
    }
}
