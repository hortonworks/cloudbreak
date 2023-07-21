package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;

class CbSaltUpdateBeforeRotationEventProviderTest {

    private CbSaltUpdateBeforeRotationEventProvider underTest = new CbSaltUpdateBeforeRotationEventProvider();

    @Test
    public void testTriggerEventProvided() {
        Selectable triggerEvent = underTest.getTriggerEvent(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, null, null));

        assertInstanceOf(StackEvent.class, triggerEvent);
        StackEvent stackEvent = (StackEvent) triggerEvent;
        assertEquals(1L, stackEvent.getResourceId());
        assertEquals("SALT_UPDATE_TRIGGER_EVENT", stackEvent.getSelector());
    }
}