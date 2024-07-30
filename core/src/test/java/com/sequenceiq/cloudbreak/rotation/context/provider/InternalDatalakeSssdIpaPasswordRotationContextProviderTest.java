package com.sequenceiq.cloudbreak.rotation.context.provider;

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

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class InternalDatalakeSssdIpaPasswordRotationContextProviderTest extends AbstractSssdIpaPasswordRotationContextProviderTest {

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private InternalDatalakeSssdIpaPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        mockGetSssdPillar();
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("crn");
        assertEquals(1, contexts.size());
        assertTrue(((SaltPillarRotationContext) contexts.get(CloudbreakSecretRotationStep.SALT_PILLAR))
                .getServicePillarGenerator().apply(stackDto).containsKey("sssd"));
    }
}
