package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@ExtendWith(MockitoExtension.class)
class DatahubExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private DatabaseRootPasswordSaltPillarGenerator databaseRootPasswordSaltPillarGenerator;

    @InjectMocks
    private DatahubExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertThat(contexts).hasSize(2);
        assertThat(contexts).containsOnlyKeys(CloudbreakSecretRotationStep.SALT_PILLAR, CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING);

        assertThat(contexts.get(CloudbreakSecretRotationStep.SALT_PILLAR)).isInstanceOf(SaltPillarRotationContext.class);
        SaltPillarRotationContext saltPillarRotationContext = (SaltPillarRotationContext) contexts.get(CloudbreakSecretRotationStep.SALT_PILLAR);
        assertEquals(RESOURCE_CRN, saltPillarRotationContext.getResourceCrn());
        assertEquals(databaseRootPasswordSaltPillarGenerator, saltPillarRotationContext.getServicePillarGenerator());

        assertThat(contexts.get(CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING)).isInstanceOf(PollerRotationContext.class);
        PollerRotationContext redbeamsPollerRotationContext = (PollerRotationContext) contexts.get(CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING);
        assertEquals(RESOURCE_CRN, redbeamsPollerRotationContext.getResourceCrn());
        assertEquals(RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, redbeamsPollerRotationContext.getSecretType());
    }

}