package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootConfigRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.saltboot.SaltBootUpdateConfiguration;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;

@ExtendWith(MockitoExtension.class)
class SaltBootConfigRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String NEW_PASSWORD = "newPassword";

    private static final String OLD_PASSWORD = "oldPassword";

    private static final String NEW_PRIVATE_KEY = newKey();

    private static final String OLD_PRIVATE_KEY = newKey();

    private static final String CONFIG_FOLDER = "/folder";

    private static final String CONFIG_FILE = "config.conf";

    private static final String NEW_CONFIG = "a=1";

    private static final String OLD_CONFIG = "a=0";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @InjectMocks
    private SaltBootConfigRotationExecutor underTest;

    @Captor
    private ArgumentCaptor<GatewayConfig> gatewayConfigCaptor;

    private GatewayConfig gatewayConfig = GatewayConfig.builder().build();

    @BeforeEach
    public void setUp() {
        lenient().when(secretRotationProgressService.latestStep(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void rotateWithOldSaltBootSecrets() throws Exception {
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        underTest.rotate(context);

        verifyFileUpload(gatewayConfigCaptor, NEW_CONFIG);
        GatewayConfig usedGatewayConfig = gatewayConfigCaptor.getValue();
        assertGateWayConfig(usedGatewayConfig, OLD_PASSWORD, OLD_PRIVATE_KEY);
        verifyRestartStatesAreApplied();
    }

    @Test
    public void rotateWithNewSaltBootSecrets() throws Exception {
        givenSaltBootTestFailsFirstThenCompletes();
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        underTest.rotate(context);

        verifyFileUpload(gatewayConfigCaptor, NEW_CONFIG);
        GatewayConfig usedGatewayConfig = gatewayConfigCaptor.getValue();
        assertGateWayConfig(usedGatewayConfig, NEW_PASSWORD, NEW_PRIVATE_KEY);
        verifyRestartStatesAreApplied();
    }

    @Test
    public void rotateFailsWhenSaltBootIsNotReachable() throws Exception {
        givenSaltBootTestFails();
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        SecretRotationException exception = assertThrows(SecretRotationException.class, () -> underTest.rotate(context));

        assertEquals("Salt boot is not reachable with old nor with new secrets. /folder/config.conf service config can't be updated.", exception.getMessage());
    }

    @Test
    public void rollbackWithOldSaltBootSecrets() throws Exception {
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        underTest.rollback(context);

        verifyFileUpload(gatewayConfigCaptor, OLD_CONFIG);
        GatewayConfig usedGatewayConfig = gatewayConfigCaptor.getValue();
        assertGateWayConfig(usedGatewayConfig, OLD_PASSWORD, OLD_PRIVATE_KEY);
        verifyRestartStatesAreApplied();
    }

    @Test
    public void rollbackWithNewSaltBootSecrets() throws Exception {
        givenSaltBootTestFailsFirstThenCompletes();
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        underTest.rollback(context);

        verifyFileUpload(gatewayConfigCaptor, OLD_CONFIG);
        GatewayConfig usedGatewayConfig = gatewayConfigCaptor.getValue();
        assertGateWayConfig(usedGatewayConfig, NEW_PASSWORD, NEW_PRIVATE_KEY);
        verifyRestartStatesAreApplied();
    }

    @Test
    public void rollbackFailsWhenSaltBootIsNotReachable() throws Exception {
        givenSaltBootTestFails();
        SaltBootConfigRotationContext context = getServiceConfigRotationContext();

        SecretRotationException exception = assertThrows(SecretRotationException.class, () -> underTest.rollback(context));

        assertEquals("Salt boot is not reachable with old nor with new secrets. /folder/config.conf service config can't be updated.", exception.getMessage());
    }

    private static String newKey() {
        return BaseEncoding.base64().encode(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()).getBytes());
    }

    private void verifyFileUpload(ArgumentCaptor<GatewayConfig> captor, String config) throws CloudbreakOrchestratorFailedException {
        verify(hostOrchestrator, times(1)).uploadFile(
                captor.capture(),
                any(),
                any(),
                eq(CONFIG_FOLDER),
                eq(CONFIG_FILE),
                eq(config.getBytes(StandardCharsets.UTF_8)));
    }

    private void givenSaltBootTestFails() throws CloudbreakOrchestratorFailedException {
        doThrow(CloudbreakOrchestratorFailedException.class)
                .when(hostOrchestrator).uploadFile(
                        any(),
                        any(),
                        any(),
                        eq("/tmp"),
                        matches("saltboottest-(\\d+)"),
                        any());
    }

    private void givenSaltBootTestFailsFirstThenCompletes() throws CloudbreakOrchestratorFailedException {
        doThrow(CloudbreakOrchestratorFailedException.class)
                .doNothing()
                .when(hostOrchestrator).uploadFile(
                        any(),
                        any(),
                        any(),
                        eq("/tmp"),
                        matches("saltboottest-(\\d+)"),
                        any());
    }

    private void verifyRestartStatesAreApplied() throws CloudbreakOrchestratorFailedException {
        verify(hostOrchestrator).executeSaltState(any(), any(), eq(List.of("stop", "start")), any(), any(), any());
    }

    private void assertGateWayConfig(GatewayConfig gatewayConfig, String expectedPassword, String expectedSigningKey) {
        assertEquals(expectedPassword, gatewayConfig.getSaltBootPassword());
        assertEquals(new String(BaseEncoding.base64().decode(expectedSigningKey)), gatewayConfig.getSignatureKey());
    }

    private SaltBootConfigRotationContext getServiceConfigRotationContext() {
        return new SaltBootConfigRotationContext(RESOURCE_CRN) {
            @Override
            public SaltBootUpdateConfiguration getServiceUpdateConfiguration() {
                return new SaltBootUpdateConfiguration(
                        gatewayConfig,
                        OLD_PASSWORD,
                        NEW_PASSWORD,
                        OLD_PRIVATE_KEY,
                        NEW_PRIVATE_KEY,
                        CONFIG_FOLDER,
                        CONFIG_FILE,
                        NEW_CONFIG,
                        OLD_CONFIG,
                        Set.of("0.0.0.0"),
                        Set.of("host0"),
                        List.of("stop", "start"),
                        3,
                        mock(ExitCriteriaModel.class));
            }
        };
    }
}