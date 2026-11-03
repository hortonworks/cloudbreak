package com.sequenceiq.cloudbreak.core.flow2.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

class DatalakeReachabilityValidationTriggerEventTest {

    private static final String SELECTOR = "DATALAKE_REACHABILITY_VALIDATION_EVENT";

    private static final Long STACK_ID = 1L;

    @Test
    void equalsEventWhenTheFailureStatusIsTheSame() {
        DatalakeReachabilityValidationTriggerEvent underTest = new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID,
                DetailedStackStatus.REPAIR_FAILED);

        assertTrue(underTest.equalsEvent(new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID, DetailedStackStatus.REPAIR_FAILED)));
    }

    @Test
    void doesNotEqualEventWhenTheFailureStatusDiffers() {
        DatalakeReachabilityValidationTriggerEvent underTest = new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID,
                DetailedStackStatus.REPAIR_FAILED);

        assertFalse(underTest.equalsEvent(new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID, DetailedStackStatus.UPSCALE_FAILED)));
    }

    @Test
    void doesNotEqualEventOfAnotherClass() {
        DatalakeReachabilityValidationTriggerEvent underTest = new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID,
                DetailedStackStatus.REPAIR_FAILED);

        assertFalse(underTest.equalsEvent(new StackEvent(SELECTOR, STACK_ID)));
    }

    @Test
    void exposesTheFailureStatus() {
        DatalakeReachabilityValidationTriggerEvent underTest = new DatalakeReachabilityValidationTriggerEvent(SELECTOR, STACK_ID,
                DetailedStackStatus.UPSCALE_FAILED);

        assertEquals(DetailedStackStatus.UPSCALE_FAILED, underTest.getFailureStatus());
        assertTrue(underTest.toString().contains("failureStatus=UPSCALE_FAILED"));
    }
}
