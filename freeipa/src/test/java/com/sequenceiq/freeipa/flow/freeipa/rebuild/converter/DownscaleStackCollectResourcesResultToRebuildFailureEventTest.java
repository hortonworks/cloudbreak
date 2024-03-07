package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;

class DownscaleStackCollectResourcesResultToRebuildFailureEventTest {

    private DownscaleStackCollectResourcesResultToRebuildFailureEvent underTest = new DownscaleStackCollectResourcesResultToRebuildFailureEvent();

    @Test
    void canConvert() {
        assertTrue(underTest.canConvert(DownscaleStackCollectResourcesResult.class));
    }

    @Test
    void convert() {
        DownscaleStackCollectResourcesResult payload = new DownscaleStackCollectResourcesResult("asdf", new CloudbreakException("fda"), 3L);

        RebuildFailureEvent result = underTest.convert(payload);

        assertEquals(payload.getResourceId(), result.getResourceId());
        assertEquals(payload.getErrorDetails(), result.getException());
    }
}