package com.sequenceiq.cloudbreak.core.flow2.restart;

import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import org.springframework.stereotype.Component;
import reactor.bus.EventBus;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_CHAIN_ID;
import static com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.FLOW_ID;

@Component("DefaultRestartAction")
public class DefaultRestartAction implements RestartAction {

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public void restart(String flowId, String flowChainId, String event, Object payload) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(FLOW_ID, flowId);
        if (flowChainId != null) {
            headers.put(FLOW_CHAIN_ID, flowChainId);
        }
        eventBus.notify(event, eventFactory.createEventWithErrHandler(headers, payload));
    }
}
