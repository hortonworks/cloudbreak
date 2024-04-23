package com.sequenceiq.cloudbreak.structuredevent.util;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.flow.core.FlowState;

public class FlowStateUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowStateUtil.class);

    private FlowStateUtil() {
    }

    public static Enum<? extends FlowState> getFlowStateEnum(Class<? extends Enum> stateClass, FlowDetails flow) {
        if (flow == null) {
            LOGGER.warn("Flow details is null!");
            return null;
        }
        String flowState = flow.getNextFlowState();
        try {
            Enum flowStateEnum = EnumUtils.getEnum(stateClass, flowState);
            if (flowStateEnum == null) {
                if ("FLOW_CANCEL".equals(flow.getFlowEvent())) {
                    LOGGER.debug("Flow was cancelled.");
                } else {
                    LOGGER.warn("Missing flow state enum for type: {}, state: {}", stateClass, flow.getNextFlowState());
                }
            }
            return flowStateEnum;
        } catch (Exception e) {
            LOGGER.warn("Cannot get enum for class: {}, value: {}", stateClass, flowState, e);
            return null;
        }
    }
}