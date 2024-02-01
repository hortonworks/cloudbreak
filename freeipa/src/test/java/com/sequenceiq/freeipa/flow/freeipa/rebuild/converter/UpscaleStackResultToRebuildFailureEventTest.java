package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;

class UpscaleStackResultToRebuildFailureEventTest {

    private UpscaleStackResultToRebuildFailureEvent underTest = new UpscaleStackResultToRebuildFailureEvent();

    @Test
    void canConvert() {
        assertTrue(underTest.canConvert(UpscaleStackResult.class));
    }

    @Test
    void convert() {
        Exception exception = new Exception("asdf");
        RebuildFailureEvent result = underTest.convert(new UpscaleStackResult("hmm", exception, 3L));

        assertEquals(3L, result.getResourceId());
        assertEquals(exception, result.getException());
    }
}