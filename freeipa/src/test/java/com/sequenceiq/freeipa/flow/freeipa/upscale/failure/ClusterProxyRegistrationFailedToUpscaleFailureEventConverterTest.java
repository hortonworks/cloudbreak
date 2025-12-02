package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;

public class ClusterProxyRegistrationFailedToUpscaleFailureEventConverterTest {

    private static final long STACK_ID = 1L;

    private final ClusterProxyRegistrationFailedToUpscaleFailureEventConverter underTest = new ClusterProxyRegistrationFailedToUpscaleFailureEventConverter();

    @Test
    void testCanConvert() {
        assertTrue(underTest.canConvert(ClusterProxyRegistrationFailed.class));
        assertFalse(underTest.canConvert(StackFailureEvent.class));
    }

    @Test
    void testConvert() {
        RuntimeException caughtException = new RuntimeException("my exception message");
        ClusterProxyRegistrationFailed clusterProxyRegistrationFailed =
                new ClusterProxyRegistrationFailed(STACK_ID, caughtException, ERROR);

        UpscaleFailureEvent upscaleFailureEvent = underTest.convert(clusterProxyRegistrationFailed);

        assertEquals(STACK_ID, upscaleFailureEvent.getResourceId());
        assertEquals("ClusterProxyRegistration", upscaleFailureEvent.getFailedPhase());
        assertTrue(upscaleFailureEvent.getFailureDetails().isEmpty());
        assertTrue(upscaleFailureEvent.getSuccess().isEmpty());
        assertEquals(caughtException, upscaleFailureEvent.getException());
    }

}
