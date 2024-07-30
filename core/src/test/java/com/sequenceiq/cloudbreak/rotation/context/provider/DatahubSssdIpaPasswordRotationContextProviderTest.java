package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class DatahubSssdIpaPasswordRotationContextProviderTest extends AbstractSssdIpaPasswordRotationContextProviderTest {

    @Mock
    private StackDtoService stackDtoService;

    @InjectMocks
    private DatahubSssdIpaPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        mockGetSssdPillar();
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getName()).thenReturn("stack");
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("crn");
        assertEquals(2, contexts.size());
        assertEquals("stack",
                ((PollerRotationContext) contexts.get(CommonSecretRotationStep.FREEIPA_ROTATE_POLLING)).getAdditionalProperties().get("CLUSTER_NAME"));
        assertTrue(((SaltPillarRotationContext) contexts.get(CloudbreakSecretRotationStep.SALT_PILLAR))
                .getServicePillarGenerator().apply(stackDto).containsKey("sssd"));
    }
}
