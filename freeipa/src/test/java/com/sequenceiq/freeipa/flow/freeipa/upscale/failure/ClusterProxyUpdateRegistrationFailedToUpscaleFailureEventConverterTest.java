package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverterTest {

    // can convert ClusterProxyUpdateRegistrationFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter converter = new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(ClusterProxyUpdateRegistrationFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with empty success set
    @Test
    public void testReturnsUpscaleFailureEventWithEmptySuccessSet() {
        ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter converter = new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        ClusterProxyUpdateRegistrationFailed payload = new ClusterProxyUpdateRegistrationFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Updating cluster proxy", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());
        assertTrue(result.getFailureDetails().isEmpty());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter converter = new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of ClusterProxyUpdateRegistrationFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter converter = new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter converter = new ClusterProxyUpdateRegistrationFailedToUpscaleFailureEventConverter();
        ClusterProxyUpdateRegistrationFailed payload = new ClusterProxyUpdateRegistrationFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}