package com.sequenceiq.flow.core.metrics;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.core.FlowState;

public class FlowStateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStateUtil.class);

    private FlowStateUtil() {
    }

    public static Enum<? extends FlowState> getFlowStateEnum(Class<? extends Enum> stateClass, String nextFlowState, String flowEvent) {
        if (nextFlowState == null) {
            LOGGER.warn("Next flow state is null!");
            return null;
        }
        try {
            Enum<? extends FlowState> flowStateEnum = EnumUtils.getEnum(stateClass, nextFlowState);
            if (flowStateEnum == null) {
                if ("FLOW_CANCEL".equals(flowEvent)) {
                    LOGGER.debug("Flow was cancelled.");
                } else {
                    LOGGER.warn("Missing flow state enum for type: {}, state: {}", stateClass, nextFlowState);
                }
            }
            return flowStateEnum;
        } catch (Exception e) {
            LOGGER.warn("Cannot get enum for class: {}, value: {}", stateClass, nextFlowState, e);
            return null;
        }
    }
}