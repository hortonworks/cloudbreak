package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

class DatalakeStackEncryptionKeysRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private final DatalakeStackEncryptionKeysRotationContextProvider underTest = new DatalakeStackEncryptionKeysRotationContextProvider();

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, ? extends RotationContext> result = underTest.getContexts(RESOURCE_CRN);
        PollerRotationContext pollerRotationContext = (PollerRotationContext) result.get(CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING);

        assertEquals(1, result.size());
        assertEquals(RESOURCE_CRN, pollerRotationContext.getResourceCrn());
        assertEquals(CloudbreakSecretType.STACK_ENCRYPTION_KEYS, pollerRotationContext.getSecretType());
    }

    @Test
    void testGetSecret() {
        assertEquals(DatalakeSecretType.STACK_ENCRYPTION_KEYS, underTest.getSecret());
    }
}
