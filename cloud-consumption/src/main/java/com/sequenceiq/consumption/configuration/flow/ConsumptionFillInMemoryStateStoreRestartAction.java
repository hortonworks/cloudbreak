package com.sequenceiq.consumption.configuration.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.restart.DefaultRestartAction;

@Component("ConsumptionFillInMemoryStateStoreRestartAction")
public class ConsumptionFillInMemoryStateStoreRestartAction extends DefaultRestartAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionFillInMemoryStateStoreRestartAction.class);

    @Override
    public void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload) {
        LOGGER.debug("Restoring MDC context and InMemoryStateStore entry for flow: '{}', flow chain: '{}', event: '{}'", flowParameters.getFlowId(),
                flowChainId, event);
    }
}
