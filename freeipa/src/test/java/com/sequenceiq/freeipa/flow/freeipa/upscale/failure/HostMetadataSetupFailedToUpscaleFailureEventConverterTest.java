package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.hostmetadatasetup.HostMetadataSetupFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class HostMetadataSetupFailedToUpscaleFailureEventConverterTest {

    // can convert HostMetadataSetupFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        HostMetadataSetupFailedToUpscaleFailureEventConverter converter = new HostMetadataSetupFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(HostMetadataSetupFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with empty success set
    @Test
    public void testReturnsUpscaleFailureEventWithEmptySuccessSet() {
        HostMetadataSetupFailedToUpscaleFailureEventConverter converter = new HostMetadataSetupFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        HostMetadataSetupFailed payload = new HostMetadataSetupFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Host metadata setup", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());
        assertTrue(result.getFailureDetails().isEmpty());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        HostMetadataSetupFailedToUpscaleFailureEventConverter converter = new HostMetadataSetupFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of HostMetadataSetupFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        HostMetadataSetupFailedToUpscaleFailureEventConverter converter = new HostMetadataSetupFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        HostMetadataSetupFailedToUpscaleFailureEventConverter converter = new HostMetadataSetupFailedToUpscaleFailureEventConverter();
        HostMetadataSetupFailed payload = new HostMetadataSetupFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}