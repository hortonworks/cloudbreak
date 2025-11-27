package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.SaltUpdateTriggerEvent;

class StackEventToSaltUpdateTriggerEventConverterTest {

    private StackEventToSaltUpdateTriggerEventConverter underTest = new StackEventToSaltUpdateTriggerEventConverter();

    @Test
    void testCanConvert() {
        assertTrue(underTest.canConvert(StackEvent.class));
    }

    @Test
    void testConvert() {
        String selector = "SALT_UPDATE_TRIGGER_EVENT";
        Long resourceId = 123L;
        StackEvent stackEvent = new StackEvent(selector, resourceId);

        SaltUpdateTriggerEvent result = underTest.convert(stackEvent);

        assertEquals("SALT_UPDATE_TRIGGER_EVENT", result.getSelector());
        assertEquals(resourceId, result.getResourceId());
    }
}
