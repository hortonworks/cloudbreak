package com.sequenceiq.freeipa.flow.chain;

import java.util.Map;

public interface FlowChainAwareAction {
    String FINAL_CHAIN = "FINAL_CHAIN";

    String CHAINED_ACTION = "CHAINED_ACTION";

    default void setFinalChain(Map<Object, Object> variables, Boolean finalChain) {
        variables.put(FINAL_CHAIN, finalChain);
    }

    default Boolean isFinalChain(Map<Object, Object> variables) {
        return (Boolean) variables.getOrDefault(FINAL_CHAIN, Boolean.FALSE);
    }

    default void setChainedAction(Map<Object, Object> variables, Boolean chainedAction) {
        variables.put(CHAINED_ACTION, chainedAction);
    }

    default Boolean isChainedAction(Map<Object, Object> variables) {
        return (Boolean) variables.getOrDefault(CHAINED_ACTION, Boolean.FALSE);
    }
}
