package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;

class StackFailureEventToRebuildFailureEventTest {

    private final StackFailureEventToRebuildFailureEvent underTest = new StackFailureEventToRebuildFailureEvent();

    @Test
    void canConvert() {
        assertTrue(underTest.canConvert(StackFailureEvent.class));
    }

    @Test
    void convert() {
        StackFailureEvent payload = new StackFailureEvent(6L,  new CloudbreakException("fda"), ERROR);

        RebuildFailureEvent result = underTest.convert(payload);

        assertEquals(payload.getResourceId(), result.getResourceId());
        assertEquals(payload.getException(), result.getException());
    }
}