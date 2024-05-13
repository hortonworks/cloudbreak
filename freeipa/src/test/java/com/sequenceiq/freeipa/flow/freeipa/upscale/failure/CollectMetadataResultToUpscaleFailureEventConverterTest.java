package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class CollectMetadataResultToUpscaleFailureEventConverterTest {

    // can convert CollectMetadataResult to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        CollectMetadataResultToUpscaleFailureEventConverter converter = new CollectMetadataResultToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(CollectMetadataResult.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with empty success set
    @Test
    public void testReturnsUpscaleFailureEventWithEmptySuccessSet() {
        CollectMetadataResultToUpscaleFailureEventConverter converter = new CollectMetadataResultToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        CollectMetadataResult payload = new CollectMetadataResult(exception, 1L);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Collecting metadata", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());
        assertTrue(result.getFailureDetails().isEmpty());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        CollectMetadataResultToUpscaleFailureEventConverter converter = new CollectMetadataResultToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of CollectMetadataResult, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        CollectMetadataResultToUpscaleFailureEventConverter converter = new CollectMetadataResultToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        CollectMetadataResultToUpscaleFailureEventConverter converter = new CollectMetadataResultToUpscaleFailureEventConverter();
        CollectMetadataResult payload = new CollectMetadataResult(1L, null);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}