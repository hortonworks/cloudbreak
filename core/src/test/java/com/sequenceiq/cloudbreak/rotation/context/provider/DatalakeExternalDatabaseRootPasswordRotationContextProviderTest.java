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
import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@ExtendWith(MockitoExtension.class)
class DatalakeExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private DatabaseRootPasswordSaltPillarGenerator databaseRootPasswordSaltPillarGenerator;

    @InjectMocks
    private DatalakeExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);
        assertThat(contexts).hasSize(1);
        assertThat(contexts).containsOnlyKeys(CloudbreakSecretRotationStep.SALT_PILLAR);

        RotationContext rotationContext = contexts.get(CloudbreakSecretRotationStep.SALT_PILLAR);
        assertThat(rotationContext).isInstanceOf(SaltPillarRotationContext.class);
        SaltPillarRotationContext saltPillarRotationContext = (SaltPillarRotationContext) rotationContext;
        assertEquals(RESOURCE_CRN, saltPillarRotationContext.getResourceCrn());
        assertEquals(databaseRootPasswordSaltPillarGenerator, saltPillarRotationContext.getServicePillarGenerator());
    }
}