package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackFailed;

public class ImageFallbackFailedToUpscaleFailureEventConverterTest {

    // can convert ImageFallbackFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        ImageFallbackFailedToUpscaleFailureEventConverter converter = new ImageFallbackFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(ImageFallbackFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with failedPhase "Image fallback"
    @Test
    public void testReturnsUpscaleFailureEventWithFailedPhase() {
        ImageFallbackFailedToUpscaleFailureEventConverter converter = new ImageFallbackFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception("message");
        ImageFallbackFailed payload = new ImageFallbackFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Image fallback", result.getFailedPhase());
        assertFalse(result.getFailureDetails().isEmpty());
        assertTrue(result.getFailureDetails().containsValue("message"));
        assertTrue(result.getSuccess().isEmpty());
        assertEquals(exception, result.getException());

    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        ImageFallbackFailedToUpscaleFailureEventConverter converter = new ImageFallbackFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of ImageFallbackFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        ImageFallbackFailedToUpscaleFailureEventConverter converter = new ImageFallbackFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        ImageFallbackFailedToUpscaleFailureEventConverter converter = new ImageFallbackFailedToUpscaleFailureEventConverter();
        ImageFallbackFailed payload = new ImageFallbackFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}