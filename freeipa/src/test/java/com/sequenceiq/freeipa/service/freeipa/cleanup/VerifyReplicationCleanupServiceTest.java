package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class VerifyReplicationCleanupServiceTest {

    private static final long STACK_ID = 1L;

    private static final String SALT_STATE_SLS = "freeipa.verify-replication-cleanup";

    @Mock
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private VerifyReplicationCleanupService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "timeoutSec", 600L);
        ReflectionTestUtils.setField(underTest, "intervalSec", 10L);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenHaAndNonEmptyHostsThenRunsOrchestratorState() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(
                mock(InstanceMetaData.class),
                mock(InstanceMetaData.class)
        ));
        stubStatePresent(stack, true);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParams(STACK_ID, "freeipa/verify-replication-cleanup")).thenReturn(stateParams);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com"));

        verify(orchestratorParamsProvider).createStateParams(STACK_ID, "freeipa/verify-replication-cleanup");

        ArgumentCaptor<OrchestratorStateParams> captor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator).runOrchestratorState(captor.capture());

        OrchestratorStateParams capturedParams = captor.getValue();
        Map<String, Object> outerMap = capturedParams.getStateParams();
        assertThat(outerMap).containsKey("freeipa");

        @SuppressWarnings("unchecked")
        Map<String, Object> freeipaMap = (Map<String, Object>) outerMap.get("freeipa");
        assertThat(freeipaMap).containsKey("replication_cleanup");

        @SuppressWarnings("unchecked")
        Map<String, Object> cleanupMap = (Map<String, Object>) freeipaMap.get("replication_cleanup");
        assertThat(cleanupMap.get("removed_hosts")).isEqualTo("host1.example.com");
        assertThat(cleanupMap.get("timeout_sec")).isEqualTo(600L);
        assertThat(cleanupMap.get("interval_sec")).isEqualTo(10L);

        assertThat(capturedParams.getStateRetryParams()).hasValueSatisfying(retryParams -> {
            // Poll budget must outlast the script's own timeoutSec (600s / 10s = 60 polls) plus margin, so salt never
            // interrupts a legitimately waiting script.
            assertThat(retryParams.getMaxRetry()).isEqualTo(72);
            assertThat(retryParams.getSleepTime()).isEqualTo(10_000);
            assertThat(retryParams.getMaxRetryOnError()).isEqualTo(2);
        });
    }

    @Test
    void testVerifyOnSurvivingMastersWhenOrchestratorThrowsThenPropagates() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(
                mock(InstanceMetaData.class),
                mock(InstanceMetaData.class)
        ));
        stubStatePresent(stack, true);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParams(STACK_ID, "freeipa/verify-replication-cleanup")).thenReturn(stateParams);

        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("salt failed");
        org.mockito.Mockito.doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com")));

        // The curated, actionable message must reach the flow (and thus the UI), not the raw salt dump.
        assertThat(thrown.getMessage()).contains("host1.example.com").contains("ipa-replica-manage clean-dangling-ruv");
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenEmptyHostsThenShortCircuits() throws CloudbreakOrchestratorException {
        underTest.verifyOnSurvivingMasters(STACK_ID, List.of());

        verifyNoInteractions(stackService, orchestratorParamsProvider, hostOrchestrator, gatewayConfigService);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenNullHostsThenShortCircuits() throws CloudbreakOrchestratorException {
        underTest.verifyOnSurvivingMasters(STACK_ID, null);

        verifyNoInteractions(stackService, orchestratorParamsProvider, hostOrchestrator, gatewayConfigService);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenSingleSurvivingMasterThenStillRunsOrchestratorState() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(mock(InstanceMetaData.class)));
        stubStatePresent(stack, true);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParams(STACK_ID, "freeipa/verify-replication-cleanup")).thenReturn(stateParams);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com"));

        // A lone surviving master still holds the removed peers' RUV/agreements, so cleanup must be verified.
        verify(hostOrchestrator).runOrchestratorState(any());
    }

    @Test
    void testVerifyOnSurvivingMastersWhenStateNotPresentOnClusterThenSkipsWithoutRunningOrchestrator() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(mock(InstanceMetaData.class)));
        stubStatePresent(stack, false);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com"));

        // Older cluster whose salt master predates the feature: skip rather than fail on a "state not found".
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenPresenceProbeFailsThenSkipsWithoutRunningOrchestrator() throws CloudbreakOrchestratorException {
        Stack stack = mock(Stack.class);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(mock(InstanceMetaData.class)));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(SALT_STATE_SLS), anyInt(), anyInt()))
                .thenThrow(new CloudbreakOrchestratorFailedException("cannot reach salt master"));

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of("host1.example.com"));

        // A probe failure is a precondition error, not the check itself — do not introduce a new downscale failure mode.
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    private void stubStatePresent(Stack stack, boolean present) throws CloudbreakOrchestratorException {
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(SALT_STATE_SLS), anyInt(), anyInt()))
                .thenReturn(present);
    }
}
