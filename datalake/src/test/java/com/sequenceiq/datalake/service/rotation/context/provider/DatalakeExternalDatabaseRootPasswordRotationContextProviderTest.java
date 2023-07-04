package com.sequenceiq.datalake.service.rotation.context.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@ExtendWith(MockitoExtension.class)
class DatalakeExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @InjectMocks
    private DatalakeExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertThat(contexts).containsOnlyKeys(CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING, CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING);

        assertThat(contexts.get(CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING)).isInstanceOf(PollerRotationContext.class);
        PollerRotationContext redbeamsPollerRotationContext = (PollerRotationContext) contexts.get(CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, redbeamsPollerRotationContext.getResourceCrn());
        assertEquals(RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, redbeamsPollerRotationContext.getSecretType());

        assertThat(contexts.get(CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING)).isInstanceOf(PollerRotationContext.class);
        PollerRotationContext pollerRotationContext =
                (PollerRotationContext) contexts.get(CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, pollerRotationContext.getResourceCrn());
        assertEquals(CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD, pollerRotationContext.getSecretType());
    }

}