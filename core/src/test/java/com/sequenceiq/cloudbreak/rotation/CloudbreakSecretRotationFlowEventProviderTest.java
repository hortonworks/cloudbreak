package com.sequenceiq.cloudbreak.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_ADMIN_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.EMBEDDED_DB_SSL_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;

class CloudbreakSecretRotationFlowEventProviderTest {

    private CloudbreakSecretRotationFlowEventProvider underTest = new CloudbreakSecretRotationFlowEventProvider();

    @Test
    public void testGetPostFlowEvents() {
        assertTrue(underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(SALT_BOOT_SECRETS), null, null)).isEmpty());
        Set<Selectable> postFlowEvents = underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(CM_INTERMEDIATE_CA_CERT), null, null));
        assertFalse(postFlowEvents.isEmpty());
        assertEquals(postFlowEvents.iterator().next().getSelector(), ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event());
    }

    @Test
    public void testTriggerEventProvided() {
        Selectable triggerEvent = underTest.getSaltUpdateTriggerEvent(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, null, null, null));

        assertInstanceOf(StackEvent.class, triggerEvent);
        StackEvent stackEvent = (StackEvent) triggerEvent;
        assertEquals(1L, stackEvent.getResourceId());
        assertEquals("SALT_UPDATE_TRIGGER_EVENT", stackEvent.getSelector());
    }

    @Test
    public void testSaltUpdateCheck() {
        assertTrue(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), null, null)));
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(CM_ADMIN_PASSWORD), ROTATE, null)));
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), ROTATE, null)));
    }

    @Test
    public void testSaltHighstateCheck() {
        assertFalse(underTest.skipSaltHighstate(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), null, null)));
        assertTrue(underTest.skipSaltHighstate(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(EMBEDDED_DB_SSL_CERT), null, null)));
    }
}