package com.sequenceiq.freeipa.flow.freeipa.rebuild.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.instance.InstanceFailureEvent;

class InstanceFailureEventToRebuildFailureEventTest {

    private final InstanceFailureEventToRebuildFailureEvent underTest = new InstanceFailureEventToRebuildFailureEvent();

    @Test
    void canConvert() {
        underTest.canConvert(InstanceFailureEvent.class);
    }

    @Test
    void convert() {
        InstanceFailureEvent payload = new InstanceFailureEvent(3L, new Exception("asdf"), List.of());

        RebuildFailureEvent result = underTest.convert(payload);

        assertEquals(payload.getResourceId(), result.getResourceId());
        assertEquals(payload.getException(), result.getException());
    }
}