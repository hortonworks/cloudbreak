package com.sequenceiq.flow.core.config;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowTriggerCondition;
import com.sequenceiq.flow.core.RestartAction;

public interface FlowConfiguration<E extends FlowEvent> {

    Flow createFlow(String flowId, String flowChainId, Long stackId, String flowChainType);

    FlowTriggerCondition getFlowTriggerCondition();

    E[] getEvents();

    E[] getInitEvents();

    RestartAction getRestartAction(String event);

    String getDisplayName();

    /**
     * Obtain flow config operation type.
     * Override this function to use a not UNKNOWN value for the flow config.
     * If flow is in a flow chain, operation type of the flow chain will override this value.
     */
    default OperationType getFlowOperationType() {
        return OperationType.UNKNOWN;
    }

    default FlowFinalizerCallback getFinalizerCallBack() {
        return new FlowFinalizerCallback() {
            @Override
            protected void doFinalize(Long resourceId) {

            }
        };
    }
}
