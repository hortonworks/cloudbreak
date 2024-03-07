package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;

class DownscaleStackResultToRebuildFailureEventTest {

    private DownscaleStackResultToRebuildFailureEvent underTest = new DownscaleStackResultToRebuildFailureEvent();

    @Test
    void canConvert() {
        assertTrue(underTest.canConvert(DownscaleStackResult.class));
    }

    @Test
    void convert() {
        DownscaleStackResult payload = new DownscaleStackResult("Asdfa", new CloudbreakException("fda"), 6L);

        RebuildFailureEvent result = underTest.convert(payload);

        assertEquals(payload.getResourceId(), result.getResourceId());
        assertEquals(payload.getErrorDetails(), result.getException());
    }
}