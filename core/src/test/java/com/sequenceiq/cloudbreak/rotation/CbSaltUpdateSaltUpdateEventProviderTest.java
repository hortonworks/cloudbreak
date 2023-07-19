package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CLUSTER_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;

class CbSaltUpdateSaltUpdateEventProviderTest {

    private CbSaltUpdateSaltUpdateEventProvider underTest = new CbSaltUpdateSaltUpdateEventProvider();

    @Test
    public void testTriggerEventProvided() {
        Selectable triggerEvent = underTest.getSaltUpdateTriggerEvent(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, null, null));

        assertInstanceOf(StackEvent.class, triggerEvent);
        StackEvent stackEvent = (StackEvent) triggerEvent;
        assertEquals(1L, stackEvent.getResourceId());
        assertEquals("SALT_UPDATE_TRIGGER_EVENT", stackEvent.getSelector());
    }

    @Test
    public void testSaltUpdateCheckIfExecutionSpecified() {
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), ROTATE)));
    }

    @Test
    public void testSaltUpdateCheckIfSecretNotRequires() {
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(CLUSTER_CB_CM_ADMIN_PASSWORD), ROTATE)));
    }

    @Test
    public void testSaltUpdateCheck() {
        assertTrue(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), null)));
    }
}