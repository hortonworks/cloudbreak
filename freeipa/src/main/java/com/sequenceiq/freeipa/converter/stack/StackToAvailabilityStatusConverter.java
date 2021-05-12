package com.sequenceiq.freeipa.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@Component
public class StackToAvailabilityStatusConverter {

    public AvailabilityStatus convert(Stack stack) {
        StackStatus status = stack.getStackStatus();
        if (status == null || status.getDetailedStackStatus() == null) {
            return AvailabilityStatus.UNKNOWN;
        }
        return status.getDetailedStackStatus().getAvailabilityStatus();
    }
}
