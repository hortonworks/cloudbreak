package com.sequenceiq.cloudbreak.rotation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;

@ExtendWith(MockitoExtension.class)
public class SecretRotationSaltServiceTest {

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private SecretRotationSaltService underTest;

    @Test
    void testValidateSaltPrimaryGateway() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        when(hostOrchestrator.ping(any(), any())).thenReturn(Map.of());

        underTest.validateSaltPrimaryGateway(new StackDto());

        verify(hostOrchestrator).ping(any(), any());
    }

    @Test
    void testValidateSalt() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(hostOrchestrator.ping(any())).thenReturn(Map.of());

        underTest.validateSalt(new StackDto());

        verify(hostOrchestrator).ping(any());
    }

    @Test
    void testExecuteSaltState() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(exitCriteriaProvider.get(any())).thenReturn(new ClusterDeletionBasedExitCriteriaModel(null, null));
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeSaltState(new StackDto(), Set.of(), List.of());

        verify(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testRefreshPillars() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        when(exitCriteriaProvider.get(any())).thenReturn(new ClusterDeletionBasedExitCriteriaModel(null, null));
        doNothing().when(hostOrchestrator).saveCustomPillars(any(), any(), any());

        underTest.updateSaltPillar(new StackDto(), Map.of());

        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
    }
}
