package com.sequenceiq.freeipa.flow;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public interface OperationAwareAction {
    String OPERATION_ID = "OPERATION_ID";

    default void addMdcOperationIdIfPresent(Map<Object, Object> varialbes) {
        String operationId = getOperationId(varialbes);
        if (StringUtils.isNotBlank(operationId)) {
            MDCBuilder.addOperationId(operationId);
        }
    }

    default void setOperationId(Map<Object, Object> variables, String operationId) {
        if (StringUtils.isNotBlank(operationId)) {
            variables.put(OPERATION_ID, operationId);
            addMdcOperationIdIfPresent(variables);
        }
    }

    default String getOperationId(Map<Object, Object> variables) {
        return (String) variables.get(OPERATION_ID);
    }

    default boolean isOperationIdSet(Map<Object, Object> variables) {
        return variables.containsKey(OPERATION_ID);
    }
}
