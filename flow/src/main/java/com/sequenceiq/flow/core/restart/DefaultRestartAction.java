package com.sequenceiq.flow.core.restart;

import static com.sequenceiq.flow.core.FlowConstants.FLOW_CHAIN_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_CONTEXTPARAMS_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_ID;
import static com.sequenceiq.flow.core.FlowConstants.FLOW_TRIGGER_USERCRN;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.flow.core.RestartContext;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

/**
 * Please consider using a different, service specific one which sets the MdcContext
 * See: FillInMemoryStateStoreRestartAction or InitializeMDCContextRestartAction
 */
@Component("DefaultRestartAction")
public class DefaultRestartAction implements RestartAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRestartAction.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    @Lazy
    private FlowChains flowChains;

    @Override
    public void restart(RestartContext restartContext, Object payload) {
        LOGGER.info("Restarting flow with {}", restartContext);
        doBeforeRestart(restartContext, payload);
        if (restartContext.getFlowId() != null) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(FLOW_ID, restartContext.getFlowId());
            putIfNotNull(headers, FLOW_TRIGGER_USERCRN, restartContext.getFlowTriggerUserCrn());
            putIfNotNull(headers, FLOW_CHAIN_ID, restartContext.getFlowChainId());
            putIfNotNull(headers, FLOW_CONTEXTPARAMS_ID, restartContext.getContextParams());
            eventBus.notify(restartContext.getEvent(), eventFactory.createEventWithErrHandler(headers, payload));
        } else if (restartContext.getFlowChainId() != null) {
            flowChains.triggerNextFlow(
                    restartContext.getFlowChainId(),
                    restartContext.getFlowTriggerUserCrn(),
                    restartContext.getContextParams(),
                    restartContext.getFlowOperationType(),
                    Optional.empty());
        }
    }

    public void doBeforeRestart(RestartContext restartContext, Object payload) {

    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
