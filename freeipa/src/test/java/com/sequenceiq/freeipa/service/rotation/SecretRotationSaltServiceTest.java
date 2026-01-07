package com.sequenceiq.freeipa.service.rotation;

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

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;

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

    @Mock
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @InjectMocks
    private SecretRotationSaltService underTest;

    @Test
    void testValidateSalt() throws CloudbreakOrchestratorFailedException, SaltPingFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());

        underTest.validateSalt(new Stack());

        verify(freeIpaSaltPingService).saltPing(any(), any());
    }

    @Test
    void testExecuteSaltState() throws CloudbreakOrchestratorFailedException {
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withPrimary(true)
                        .withHostname("host")
                        .withInstanceId("instanceId")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        when(exitCriteriaProvider.get(any())).thenReturn(new StackBasedExitCriteriaModel(null));
        doNothing().when(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());

        underTest.executeSaltState(new Stack(), Set.of(), List.of());

        verify(hostOrchestrator).executeSaltState(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testRefreshPillars() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        when(exitCriteriaProvider.get(any())).thenReturn(new StackBasedExitCriteriaModel(null));
        doNothing().when(hostOrchestrator).saveCustomPillars(any(), any(), any());

        underTest.updateSaltPillar(new Stack(), Map.of());

        verify(hostOrchestrator).saveCustomPillars(any(), any(), any());
    }
}
