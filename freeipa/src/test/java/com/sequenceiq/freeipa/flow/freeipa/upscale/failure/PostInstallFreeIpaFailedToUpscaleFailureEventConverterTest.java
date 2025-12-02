package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class PostInstallFreeIpaFailedToUpscaleFailureEventConverterTest {

    // can convert PostInstallFreeIpaFailed to UpscaleFailureEvent
    @Test
    public void testCanConvertPostInstallFreeIpaFailedToUpscaleFailureEvent() {
        PostInstallFreeIpaFailedToUpscaleFailureEventConverter converter = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();
        boolean result = converter.canConvert(PostInstallFreeIpaFailed.class);
        assertTrue(result);
    }

    // returns UpscaleFailureEvent with failedPhase "Post installing FreeIPA"
    @Test
    public void testReturnsUpscaleFailureEventWithFailedPhase() {
        PostInstallFreeIpaFailedToUpscaleFailureEventConverter converter = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();
        Exception exception = new Exception();
        PostInstallFreeIpaFailed payload = new PostInstallFreeIpaFailed(1L, exception, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertEquals("Post installing FreeIPA", result.getFailedPhase());
        assertTrue(result.getSuccess().isEmpty());
        assertTrue(result.getFailureDetails().isEmpty());
        assertEquals(exception, result.getException());
    }

    // payload is null, throws NullPointerException
    @Test
    public void testPayloadIsNullThrowsNullPointerException() {
        PostInstallFreeIpaFailedToUpscaleFailureEventConverter converter = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();
        assertThrows(NullPointerException.class, () -> converter.convert(null));
    }

    // payload is not an instance of PostInstallFreeIpaFailed, returns exception
    @Test
    public void testPayloadIsNotInstanceOfPostInstallFreeIpaFailedReturnsNull() {
        PostInstallFreeIpaFailedToUpscaleFailureEventConverter converter = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();
        Object payload = new Object();
        assertThrows(ClassCastException.class, () -> converter.convert(payload));
    }

    // exception is null, UpscaleFailureEvent exception is null
    @Test
    public void testExceptionIsNullUpscaleFailureEventExceptionIsNull() {
        PostInstallFreeIpaFailedToUpscaleFailureEventConverter converter = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();
        PostInstallFreeIpaFailed payload = new PostInstallFreeIpaFailed(1L, null, ERROR);
        UpscaleFailureEvent result = converter.convert(payload);
        assertNull(result.getException());
    }
}