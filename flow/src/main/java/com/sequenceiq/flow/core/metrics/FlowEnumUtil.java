package com.sequenceiq.flow.core.metrics;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;

public class FlowEnumUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowEnumUtil.class);

    private FlowEnumUtil() {
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

    public static Enum<? extends FlowEvent> getFlowEventEnum(Class<? extends Enum> eventType, String flowEvent) {
        if (flowEvent == null) {
            LOGGER.warn("Flow event is null!");
            return null;
        }
        try {
            Enum<? extends FlowEvent> flowEventEnum = EnumUtils.getEnum(eventType, flowEvent);
            if (flowEventEnum == null) {
                LOGGER.warn("Missing flow event enum for type: {}, state: {}", eventType, flowEvent);
            }
            return flowEventEnum;
        } catch (Exception e) {
            LOGGER.warn("Cannot get enum for class: {}, value: {}", eventType, flowEvent, e);
            return null;
        }
    }
}