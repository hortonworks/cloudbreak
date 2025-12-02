package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.bootstrap.BootstrapMachinesFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class BootstrapMachinesFailedToUpscaleFailureEventConverterTest {

    // can convert BootstrapMachinesFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        BootstrapMachinesFailedToUpscaleFailureEventConverter converter = new BootstrapMachinesFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(BootstrapMachinesFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with empty success set
    @Test
    public void testReturnsUpscaleFailureEventWithEmptySuccessSet() {
        BootstrapMachinesFailedToUpscaleFailureEventConverter converter = new BootstrapMachinesFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        BootstrapMachinesFailed payload = new BootstrapMachinesFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Bootstrapping machines", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());
        assertTrue(result.getFailureDetails().isEmpty());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        BootstrapMachinesFailedToUpscaleFailureEventConverter converter = new BootstrapMachinesFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of BootstrapMachinesFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        BootstrapMachinesFailedToUpscaleFailureEventConverter converter = new BootstrapMachinesFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        BootstrapMachinesFailedToUpscaleFailureEventConverter converter = new BootstrapMachinesFailedToUpscaleFailureEventConverter();
        BootstrapMachinesFailed payload = new BootstrapMachinesFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}