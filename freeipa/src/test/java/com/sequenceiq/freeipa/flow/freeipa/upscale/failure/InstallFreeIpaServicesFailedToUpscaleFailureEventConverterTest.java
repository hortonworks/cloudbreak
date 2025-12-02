package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.services.InstallFreeIpaServicesFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class InstallFreeIpaServicesFailedToUpscaleFailureEventConverterTest {

    // can convert InstallFreeIpaServicesFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        InstallFreeIpaServicesFailedToUpscaleFailureEventConverter converter = new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(InstallFreeIpaServicesFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with empty success set
    @Test
    public void testReturnsUpscaleFailureEventWithEmptySuccessSet() {
        InstallFreeIpaServicesFailedToUpscaleFailureEventConverter converter = new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        InstallFreeIpaServicesFailed payload = new InstallFreeIpaServicesFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Installing FreeIPA services", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());
        assertTrue(result.getFailureDetails().isEmpty());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        InstallFreeIpaServicesFailedToUpscaleFailureEventConverter converter = new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of InstallFreeIpaServicesFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        InstallFreeIpaServicesFailedToUpscaleFailureEventConverter converter = new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        InstallFreeIpaServicesFailedToUpscaleFailureEventConverter converter = new InstallFreeIpaServicesFailedToUpscaleFailureEventConverter();
        InstallFreeIpaServicesFailed payload = new InstallFreeIpaServicesFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}