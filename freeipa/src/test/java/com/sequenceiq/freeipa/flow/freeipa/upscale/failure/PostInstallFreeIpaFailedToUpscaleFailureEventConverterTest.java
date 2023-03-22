package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.provision.event.postinstall.PostInstallFreeIpaFailed;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;

public class PostInstallFreeIpaFailedToUpscaleFailureEventConverterTest {

    private static final long STACK_ID = 1L;

    private final PostInstallFreeIpaFailedToUpscaleFailureEventConverter underTest = new PostInstallFreeIpaFailedToUpscaleFailureEventConverter();

    @Test
    void testCanConvert() {
        assertTrue(underTest.canConvert(PostInstallFreeIpaFailed.class));
    }

    @Test
    void testConvert() {
        PostInstallFreeIpaFailed postInstallFreeIpaFailed = new PostInstallFreeIpaFailed(STACK_ID, new RuntimeException("my Exception"));

        UpscaleFailureEvent upscaleFailureEvent = underTest.convert(postInstallFreeIpaFailed);

        assertEquals(STACK_ID, upscaleFailureEvent.getResourceId());
        assertEquals("Post installing FreeIPA", upscaleFailureEvent.getFailedPhase());
        assertTrue(upscaleFailureEvent.getFailureDetails().isEmpty());
        assertTrue(upscaleFailureEvent.getSuccess().isEmpty());
    }

}
