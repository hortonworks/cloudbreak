package com.sequenceiq.flow.core.restart;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

/**
 * Please consider using a different, service specific one which sets the MdcContext
 * See: FillInMemoryStateStoreRestartAction or InitializeMDCContextRestartAction
 */
@Component("DefaultRestartAction")
public class DefaultRestartAction implements RestartAction {

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(FlowConstants.FLOW_ID, flowParameters.getFlowId());
        if (flowParameters.getFlowTriggerUserCrn() != null) {
            headers.put(FlowConstants.FLOW_TRIGGER_USERCRN, flowParameters.getFlowTriggerUserCrn());
        }
        if (flowChainId != null) {
            headers.put(FlowConstants.FLOW_CHAIN_ID, flowChainId);
        }
        eventBus.notify(event, eventFactory.createEventWithErrHandler(headers, payload));
    }
}
