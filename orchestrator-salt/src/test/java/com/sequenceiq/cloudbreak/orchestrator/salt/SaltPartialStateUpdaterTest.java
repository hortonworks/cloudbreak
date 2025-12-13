package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltFileUpload;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

@ExtendWith(MockitoExtension.class)
public class SaltPartialStateUpdaterTest {
    private final ExitCriteriaModel exitCriteriaModel = new ExitCriteriaModel() {
    };

    private final GatewayConfig gatewayConfig = GatewayConfig.builder()
            .withConnectionAddress("1.1.1.1")
            .withPublicAddress("10.0.0.1")
            .withPrivateAddress("172.16.252.43")
            .withHostname("10-0-0-1")
            .withGatewayPort(9443)
            .withInstanceId("instanceid")
            .withServerCert("servercert")
            .withClientCert("clientcert")
            .withClientKey("clientkey")
            .withSaltPassword("saltpasswd")
            .withSaltBootPassword("saltbootpassword")
            .withSignatureKey("signkey")
            .withKnoxGatewayEnabled(false)
            .withPrimary(true)
            .withSaltMasterPrivateKey("masterPrivateKey")
            .withSaltMasterPublicKey("masterPublicKey")
            .withSaltSignPrivateKey("privatekey")
            .withSaltSignPublicKey("publickey")
            .build();

    private final List<GatewayConfig> gatewayConfigs = List.of(gatewayConfig);

    private final ArgumentCaptor<OrchestratorBootstrap> orchestratorBootstrapArgumentCaptor = ArgumentCaptor.forClass(OrchestratorBootstrap.class);

    @Mock
    private SaltService saltService;

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltRunner saltRunner;

    @InjectMocks
    private SaltPartialStateUpdater underTest;

    @Test
    void testUpdatePartialSaltDefinition() throws CloudbreakOrchestratorFailedException {
        Callable<Boolean> callable = mock(Callable.class);
        lenient().when(saltRunner.runnerWithConfiguredErrorCount(orchestratorBootstrapArgumentCaptor.capture(), any(), any())).thenReturn(callable);
        when(saltService.getPrimaryGatewayConfig(gatewayConfigs)).thenReturn(gatewayConfig);

        underTest.updatePartialSaltDefinition(new byte[0], anyList(), gatewayConfigs, exitCriteriaModel);

        List<OrchestratorBootstrap> orchestratorBootstraps = orchestratorBootstrapArgumentCaptor.getAllValues();

        for (OrchestratorBootstrap ob : orchestratorBootstraps) {
            String saltUploadTarget = ((SaltFileUpload) ob).getTargets().stream().findFirst().orElse(null);
            assertEquals(saltUploadTarget, gatewayConfig.getPrivateAddress());
        }
        verify(saltService, times(1)).getPrimaryGatewayConfig(gatewayConfigs);
        verify(saltService, times(1)).createSaltConnector(gatewayConfig);
        verify(saltRunner, times(2)).runnerWithConfiguredErrorCount(any(), any(), any());
    }
}
