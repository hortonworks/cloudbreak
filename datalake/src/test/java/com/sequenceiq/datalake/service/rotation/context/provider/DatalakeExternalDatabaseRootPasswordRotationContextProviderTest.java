package com.sequenceiq.datalake.service.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class DatalakeExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @InjectMocks
    private DatalakeExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testIsApplicable() {
        SdxCluster sdxCluster = mock(SdxCluster.class);
        when(sdxCluster.getDatabaseAvailabilityType()).thenReturn(SdxDatabaseAvailabilityType.NON_HA);
        assertTrue(underTest.isApplicable(sdxCluster));
    }

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
        assertEquals(INTERNAL_DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD, pollerRotationContext.getSecretType());
    }

}