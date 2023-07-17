package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.SALT_BOOT_SECRETS;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

class DatalakeSaltBootSecretsRotationContextProviderTest {

    private DatalakeSaltBootSecretsRotationContextProvider underTest = new DatalakeSaltBootSecretsRotationContextProvider();

    @Test
    public void testDatalakeSaltBootSecretRotationContext() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("datalakeCrn");

        RotationContext rotationContext = contexts.get(CLOUDBREAK_ROTATE_POLLING);
        assertInstanceOf(PollerRotationContext.class, rotationContext);
        PollerRotationContext pollerRotationContext = (PollerRotationContext) rotationContext;
        assertEquals(SALT_BOOT_SECRETS, pollerRotationContext.getSecretType());
        assertEquals("datalakeCrn", pollerRotationContext.getResourceCrn());
    }
}