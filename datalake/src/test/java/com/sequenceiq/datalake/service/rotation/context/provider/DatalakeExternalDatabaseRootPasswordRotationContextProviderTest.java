package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.context.CloudbreakPollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.context.RedbeamsPollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.RedbeamsSecretType;

@ExtendWith(MockitoExtension.class)
class DatalakeExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @InjectMocks
    private DatalakeExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertThat(contexts).containsOnlyKeys(SecretRotationStep.REDBEAMS_ROTATE_POLLING, SecretRotationStep.CLOUDBREAK_ROTATE_POLLING);

        assertThat(contexts.get(SecretRotationStep.REDBEAMS_ROTATE_POLLING)).isInstanceOf(RedbeamsPollerRotationContext.class);
        RedbeamsPollerRotationContext redbeamsPollerRotationContext = (RedbeamsPollerRotationContext) contexts.get(SecretRotationStep.REDBEAMS_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, redbeamsPollerRotationContext.getResourceCrn());
        assertEquals(RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, redbeamsPollerRotationContext.getSecretType());

        assertThat(contexts.get(SecretRotationStep.CLOUDBREAK_ROTATE_POLLING)).isInstanceOf(CloudbreakPollerRotationContext.class);
        CloudbreakPollerRotationContext cloudbreakPollerRotationContext =
                (CloudbreakPollerRotationContext) contexts.get(SecretRotationStep.CLOUDBREAK_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, cloudbreakPollerRotationContext.getResourceCrn());
        assertEquals(CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD, cloudbreakPollerRotationContext.getSecretType());
    }

}