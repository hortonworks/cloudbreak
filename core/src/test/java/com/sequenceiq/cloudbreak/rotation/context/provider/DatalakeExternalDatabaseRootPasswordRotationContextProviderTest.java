package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.DatabaseRootPasswordSaltPillarGenerator;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;

@ExtendWith(MockitoExtension.class)
class DatalakeExternalDatabaseRootPasswordRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private DatabaseRootPasswordSaltPillarGenerator databaseRootPasswordSaltPillarGenerator;

    @InjectMocks
    private DatalakeExternalDatabaseRootPasswordRotationContextProvider underTest;

    @Test
    void testIsApplicable() {
        StackDto stack = mock(StackDto.class);
        Database database = mock(Database.class);
        when(database.getExternalDatabaseAvailabilityType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        when(stack.getDatabase()).thenReturn(database);
        assertTrue(underTest.isApplicable(stack));
    }

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