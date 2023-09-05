package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CB_CM_ADMIN_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_DB_PASSWORD;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_INTERMEDIATE_CA_CERT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxCertRotationEvent;

class DatalakeSecretRotationFlowEventProviderTest {

    private DatalakeSecretRotationFlowEventProvider underTest = new DatalakeSecretRotationFlowEventProvider();

    @Test
    public void testGetPostFlowEvents() {
        assertTrue(underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(DATALAKE_CM_DB_PASSWORD), null)).isEmpty());
        Set<Selectable> postFlowEvents = underTest.getPostFlowEvent(new SecretRotationFlowChainTriggerEvent(null, 1L, null,
                List.of(DATALAKE_CM_INTERMEDIATE_CA_CERT), null));
        assertFalse(postFlowEvents.isEmpty());
        assertEquals(postFlowEvents.iterator().next().getSelector(), SdxCertRotationEvent.ROTATE_CERT_EVENT.event());
    }

    @Test
    public void testSaltUpdateCheckIfExecutionSpecified() {
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(DATALAKE_CM_DB_PASSWORD), ROTATE)));
    }

    @Test
    public void testSaltUpdateCheckIfSecretNotRequires() {
        assertFalse(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(DATALAKE_CB_CM_ADMIN_PASSWORD), ROTATE)));
    }

    @Test
    public void testSaltUpdateCheck() {
        assertTrue(underTest.saltUpdateNeeded(
                new SecretRotationFlowChainTriggerEvent(null, null, null, List.of(DATALAKE_CM_DB_PASSWORD), null)));
    }
}