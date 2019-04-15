package com.sequenceiq.cloudbreak.core.flow2.restart;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.core.flow2.FlowConstants;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;

import reactor.bus.EventBus;

@Component("DefaultRestartAction")
public class DefaultRestartAction implements RestartAction {

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_ID, flowId);
        if (flowChainId != null) {
            headers.put(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        }
        eventBus.notify(event, eventFactory.createEventWithErrHandler(headers, payload));
    }
}
