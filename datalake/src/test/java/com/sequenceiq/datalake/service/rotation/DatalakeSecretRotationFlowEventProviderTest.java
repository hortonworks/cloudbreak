package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.EMBEDDED_DB_SSL_CERT;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.CM_INTERMEDIATE_CA_CERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;

class DatalakeSecretRotationFlowEventProviderTest {

    private DatalakeSecretRotationFlowEventProvider underTest = new DatalakeSecretRotationFlowEventProvider();

    @Test
    public void testGetPostFlowEvents() {
        assertTrue(underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(CM_DB_PASSWORD), null, null)).isEmpty());
        Set<Selectable> postFlowEvents = underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(CM_INTERMEDIATE_CA_CERT), null, null));
        assertFalse(postFlowEvents.isEmpty());
        assertEquals(postFlowEvents.iterator().next().getSelector(), SdxCertRotationEvent.ROTATE_CERT_EVENT.event());
    }

    @Test
    public void testSaltUpdateCheck() {
        assertTrue(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(SALT_BOOT_SECRETS), null, null)));
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(CloudbreakSecretType.CM_ADMIN_PASSWORD), ROTATE, null)));
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