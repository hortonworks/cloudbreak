package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.ValidateNewInstancesHealthFailedEvent;

public class ValidateNewInstancesHealthFailedToUpscaleFailureConverterTest {

    private static final long STACK_ID = 1;

    private static final String FAILED_PHASE = "myPhase";

    private final ValidateNewInstancesHealthFailedToUpscaleFailureConverter underTest = new ValidateNewInstancesHealthFailedToUpscaleFailureConverter();

    @Test
    void testCanConvert() {
        assertTrue(underTest.canConvert(ValidateNewInstancesHealthFailedEvent.class));
    }

    @Test
    void testConvert() {
        RuntimeException myException = new RuntimeException("myException");
        Set<String> success = Set.of("successNode");
        Map<String, String> failureDetails = Map.of("failedNode", "failureDetails");
        ValidateNewInstancesHealthFailedEvent validateNewInstancesHealthFailedEvent =
                new ValidateNewInstancesHealthFailedEvent(STACK_ID, myException, FAILED_PHASE, success, failureDetails);

        UpscaleFailureEvent upscaleFailureEvent = underTest.convert(validateNewInstancesHealthFailedEvent);

        assertEquals(STACK_ID, upscaleFailureEvent.getResourceId());
        assertEquals(myException, upscaleFailureEvent.getException());
        assertEquals(FAILED_PHASE, upscaleFailureEvent.getFailedPhase());
        assertEquals(success, upscaleFailureEvent.getSuccess());
        assertEquals(failureDetails, upscaleFailureEvent.getFailureDetails());
    }

}