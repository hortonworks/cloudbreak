package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class DatahubCMIntermediateCacertRotationContextProviderTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private StackDtoService stackService;

    @InjectMocks
    private DatahubCMIntermediateCacertRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getResourceCrn()).thenReturn("crn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getAutoTlsEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn("host");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("crn");

        assertEquals(2, contexts.size());
    }
}
