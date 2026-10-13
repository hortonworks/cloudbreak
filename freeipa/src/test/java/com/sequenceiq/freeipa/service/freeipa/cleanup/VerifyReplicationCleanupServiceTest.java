package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class VerifyReplicationCleanupServiceTest {

    private static final long STACK_ID = 1L;

    private static final String SALT_STATE_SLS = "freeipa.verify-replication-cleanup";

    private static final String CONVERGENCE_SALT_STATE_SLS = "freeipa.verify-replication-convergence";

    private static final String CLEANUP_STATE = "freeipa/verify-replication-cleanup";

    private static final String CONVERGENCE_STATE = "freeipa/verify-replication-convergence";

    private static final String REMOVED_HOST = "host1.example.com";

    private static final String MASTER_FQDN = "ipa2.example.com";

    private static final String CLEANUP_LOG = "/var/log/freeipa_replication_cleanup.log";

    private static final String CONVERGENCE_LOG = "/var/log/freeipa_replication_convergence.log";

    private static final String CLEANUP_FAILED_MESSAGE = "cleanup failed message";

    private static final String CLEANUP_EXECUTION_FAILED_MESSAGE = "cleanup execution failed message";

    private static final String CONVERGENCE_FAILED_ON_SURVIVING_MASTERS_MESSAGE = "convergence failed on surviving masters message";

    private static final String CONVERGENCE_FAILED_ON_MASTER_MESSAGE = "convergence failed on master message";

    private static final String CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE = "convergence execution failed on surviving masters message";

    private static final String CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE = "convergence execution failed on master message";

    private static final String NODE = "ipaserver1.example.com";

    private static final String GATEWAY_HOSTNAME = "ipa1.example.com";

    private static final String PROBE_ERROR = "cannot reach salt master";

    private static final String NULL_JID_MESSAGE = "Failed: Orchestrator component went failed in 1 min(s), message: Salt returned null jid. "
            + "Please check the salt log.";

    /**
     * The timeout lines below are what the scripts print to stderr; "2>&1" in the salt state brings them into the reported Stdout.
     * The same lines are asserted against the real scripts in FreeIpaVerifyReplicaHealthScriptTest and
     * FreeIpaCheckReplicationCleanupScriptTest, and the state commands in FreeIpaReplicationSaltStateContractTest. Keep them in sync:
     * changing the Java timeout marker breaks this test, changing the script line breaks the script test.
     */
    private static final String CONVERGENCE_TIMEOUT_OUTPUT = "Failed to execute: {Name=/opt/salt/scripts/freeipa_verify_replica_health.sh 2>&1 "
            + "| tee -a /var/log/freeipa_replication_convergence.log && exit ${PIPESTATUS[0]}, "
            + "Stdout=FAIL: replica ipaserver1.example.com not healthy within 300s (see failing agreements)}";

    private static final String CLEANUP_TIMEOUT_OUTPUT = "Failed to execute: {Name=/opt/salt/scripts/freeipa_check_replication_cleanup.sh 2>&1 "
            + "| tee -a /var/log/freeipa_replication_cleanup.log && exit ${PIPESTATUS[0]}, "
            + "Stdout=TIMEOUT after 600s waiting for FreeIPA replication cleanup on ldap://localhost for hosts 'host1.example.com'.}";

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

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private Retry retryService;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private VerifyReplicationCleanupService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "timeoutSec", 600L);
        ReflectionTestUtils.setField(underTest, "intervalSec", 10L);
        ReflectionTestUtils.setField(underTest, "convergenceEnabled", true);
        ReflectionTestUtils.setField(underTest, "convergenceTimeoutSec", 300L);
        ReflectionTestUtils.setField(underTest, "convergenceIntervalSec", 5L);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        lenient().when(gatewayConfig.getHostname()).thenReturn(GATEWAY_HOSTNAME);
        lenient().when(retryService.testWith1SecDelayMax3Times(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
    }

    @Test
    void testVerifyOnSurvivingMastersWhenHostsRemovedThenRunsCleanupAndThenConvergenceWithTheirOwnPillarsAndRetryBudgets()
            throws Exception {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        OrchestratorStateParams cleanupParams = stubStateParams(CLEANUP_STATE);
        OrchestratorStateParams convergenceParams = stubStateParams(CONVERGENCE_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST));

        InOrder inOrder = inOrder(hostOrchestrator);
        inOrder.verify(hostOrchestrator).runOrchestratorState(cleanupParams);
        inOrder.verify(hostOrchestrator).runOrchestratorState(convergenceParams);
        verify(hostOrchestrator, times(2)).runOrchestratorState(any());
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);

        assertThat(cleanupPillar(cleanupParams)).containsOnly(
                Map.entry("removed_hosts", REMOVED_HOST), Map.entry("timeout_sec", 600L), Map.entry("interval_sec", 10L));
        assertThat(cleanupParams.getStateRetryParams()).hasValueSatisfying(retryParams -> {
            // 600s / 10s = 60 polls plus margin: the cleanup budget no longer includes the convergence timeout.
            assertThat(retryParams.getMaxRetry()).isEqualTo(72);
            assertThat(retryParams.getSleepTime()).isEqualTo(10_000);
            assertThat(retryParams.getMaxRetryOnError()).isEqualTo(2);
        });

        assertThat(convergencePillar(convergenceParams)).containsOnly(
                Map.entry("exclude_hosts", REMOVED_HOST), Map.entry("timeout_sec", 300L), Map.entry("interval_sec", 5L));
        assertThat(convergenceParams.getStateRetryParams()).hasValueSatisfying(retryParams -> {
            // 300s / 10s = 30 polls plus margin.
            assertThat(retryParams.getMaxRetry()).isEqualTo(42);
            assertThat(retryParams.getSleepTime()).isEqualTo(10_000);
            assertThat(retryParams.getMaxRetryOnError()).isEqualTo(2);
        });
    }

    @Test
    void testVerifyOnSurvivingMastersWhenMoreHostsRemovedThenBothPillarsCarryTheCommaJoinedHosts() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        OrchestratorStateParams cleanupParams = stubStateParams(CLEANUP_STATE);
        OrchestratorStateParams convergenceParams = stubStateParams(CONVERGENCE_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, List.of("host1.example.com", "host2.example.com"));

        assertThat(cleanupPillar(cleanupParams)).containsEntry("removed_hosts", "host1.example.com,host2.example.com");
        assertThat(convergencePillar(convergenceParams)).containsEntry("exclude_hosts", "host1.example.com,host2.example.com");
    }

    @ParameterizedTest
    @MethodSource("noRemovedHosts")
    void testVerifyOnSurvivingMastersWhenNoHostRemovedThenOnlyConvergenceRuns(List<String> removedHosts) throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        OrchestratorStateParams convergenceParams = stubStateParams(CONVERGENCE_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, removedHosts);

        // Nothing to clean up, but a broken surviving master must still be detected before the next replica is seeded from it.
        verify(hostOrchestrator).runOrchestratorState(convergenceParams);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        assertThat(convergencePillar(convergenceParams)).containsOnly(
                Map.entry("exclude_hosts", ""), Map.entry("timeout_sec", 300L), Map.entry("interval_sec", 5L));
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(SALT_STATE_SLS), anyInt(), anyInt());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CLEANUP_STATE);
        // The 389-ds server-list pre-check only makes sense for removed hosts.
        verifyNoInteractions(freeIpaClientFactory);
    }

    static Stream<Arguments> noRemovedHosts() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(List.of()));
    }

    @Test
    void testVerifyOnSurvivingMastersWhenScriptTimesOutThenNotConvergedErrorIsReportedAndConvergenceDoesNotRun()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        stubCleanupFailedMessage(REMOVED_HOST);
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("ipaserver1.example.com", CLEANUP_TIMEOUT_OUTPUT);
        CloudbreakOrchestratorFailedException runnerException = new CloudbreakOrchestratorFailedException(
                "Failed: Orchestrator component went failed in 10 min(s), message: Target: [ipaserver1.example.com]", nodesWithErrors);
        // Mirrors SaltOrchestrator.runOrchestratorState, which re-wraps the runner's exception without its nodesWithErrors.
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException(runnerException.getMessage(), runnerException);
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        // The curated, actionable message must reach the flow (and thus the UI), not the raw salt dump.
        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_FAILED_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenSaltFailsBeforeCheckRunsThenExecutionErrorIsReportedAndConvergenceDoesNotRun()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException(
                "Failed: Orchestrator component went failed in 1 min(s), message: Salt returned null jid. Please check the salt log.",
                ArrayListMultimap.create());
        stubCleanupExecutionFailedMessage(REMOVED_HOST, cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        // CB-35485: an orchestration failure must not be misreported as a CleanAllRUV non-convergence.
        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_EXECUTION_FAILED_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenNodeErrorsWithoutTimeoutMarkerThenExecutionErrorIsReportedAndConvergenceDoesNotRun()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("ipaserver1.example.com", "Salt minion is not responding.");
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("minion did not return", nodesWithErrors);
        stubCleanupExecutionFailedMessage("host1.example.com, host2.example.com", cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of("host1.example.com", "host2.example.com")));

        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_EXECUTION_FAILED_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenCleanupFailsWithoutNodeErrorsThenExecutionErrorIsReportedAndConvergenceDoesNotRun()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("salt failed");
        stubCleanupExecutionFailedMessage(REMOVED_HOST, cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_EXECUTION_FAILED_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenCleanupTimesOutWithMoreHostsThenTheMessageGetsTheHostsJoinedWithCommaAndSpace()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        stubCleanupFailedMessage("host1.example.com, host2.example.com");
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("ipaserver1.example.com", CLEANUP_TIMEOUT_OUTPUT);
        doThrow(new CloudbreakOrchestratorFailedException("salt failed", nodesWithErrors)).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of("host1.example.com", "host2.example.com")));

        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_FAILED_MESSAGE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceTimesOutAfterCleanupThenNotConvergedErrorIsReported() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubStateParams(CLEANUP_STATE);
        stubStateParams(CONVERGENCE_STATE);
        stubConvergenceFailedOnSurvivingMastersMessage();
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError(CONVERGENCE_TIMEOUT_OUTPUT);
        doNothing().doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
        verify(hostOrchestrator, times(2)).runOrchestratorState(any());
    }

    @Test
    void testVerifyOnSurvivingMastersWhenNoHostRemovedAndConvergenceTimesOutThenNotConvergedErrorIsReported()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubStateParams(CONVERGENCE_STATE);
        stubConvergenceFailedOnSurvivingMastersMessage();
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError(CONVERGENCE_TIMEOUT_OUTPUT);
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of()));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceSaltReturnsNullJidThenExecutionErrorIsReported() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubStateParams(CONVERGENCE_STATE);
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException(NULL_JID_MESSAGE, ArrayListMultimap.create());
        stubConvergenceExecutionFailedOnSurvivingMastersMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of()));

        // An orchestration failure means the check never ran: it must not be reported as a replication non-convergence.
        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceNodeErrorsWithoutTimeoutMarkerThenExecutionErrorIsReported()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubStateParams(CONVERGENCE_STATE);
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError("Salt minion is not responding.");
        stubConvergenceExecutionFailedOnSurvivingMastersMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of()));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceRunSeesCleanupTimeoutMarkerThenItIsNotTreatedAsConvergenceTimeout()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubStateParams(CONVERGENCE_STATE);
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError(CLEANUP_TIMEOUT_OUTPUT);
        stubConvergenceExecutionFailedOnSurvivingMastersMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorException thrown = assertThrows(CloudbreakOrchestratorException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, List.of()));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenCleanupStateMissingThenConvergenceStillRuns() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, false);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        OrchestratorStateParams convergenceParams = stubStateParams(CONVERGENCE_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST));

        // Older cluster whose salt master predates the cleanup state: skip it rather than fail on a "state not found".
        verify(hostOrchestrator, times(1)).runOrchestratorState(convergenceParams);
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CLEANUP_STATE);
        verifyNoInteractions(freeIpaClientFactory);
        assertThat(convergencePillar(convergenceParams)).containsEntry("exclude_hosts", REMOVED_HOST);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceStateMissingThenOnlyCleanupRuns() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, false);
        OrchestratorStateParams cleanupParams = stubStateParams(CLEANUP_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST));

        verify(hostOrchestrator, times(1)).runOrchestratorState(cleanupParams);
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenNoHostRemovedAndConvergenceStateMissingThenSkips() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, false);

        underTest.verifyOnSurvivingMasters(STACK_ID, List.of());

        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenBothStatesMissingThenSkipsWithoutRunningOrchestrator() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, false);
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, false);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST));

        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceDisabledThenOnlyCleanupRunsAndConvergenceStateIsNotProbed()
            throws CloudbreakOrchestratorException {
        ReflectionTestUtils.setField(underTest, "convergenceEnabled", false);
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        OrchestratorStateParams cleanupParams = stubStateParams(CLEANUP_STATE);

        underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST));

        verify(hostOrchestrator, times(1)).runOrchestratorState(cleanupParams);
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceDisabledAndNoHostRemovedThenNothingRuns() throws CloudbreakOrchestratorException {
        ReflectionTestUtils.setField(underTest, "convergenceEnabled", false);
        stubStack();

        underTest.verifyOnSurvivingMasters(STACK_ID, List.of());

        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), any(), anyInt(), anyInt());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenCleanupProbeFailsThenThrowsCleanupExecutionErrorAndNothingElseRuns() throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsProbeFails(SALT_STATE_SLS);
        stubCleanupExecutionFailedMessage(REMOVED_HOST, probeFailureMessage(SALT_STATE_SLS));

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        // An unknown state is not a missing state: skipping would let the downscale go on without the replication being verified.
        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_EXECUTION_FAILED_MESSAGE);
        assertProbeFailureCause(thrown, SALT_STATE_SLS);
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenConvergenceProbeFailsThenCleanupRunsAndThrowsConvergenceExecutionErrorWithoutRunningConvergence()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsPresent(SALT_STATE_SLS, true);
        stubSlsProbeFails(CONVERGENCE_SALT_STATE_SLS);
        OrchestratorStateParams cleanupParams = stubStateParams(CLEANUP_STATE);
        stubConvergenceExecutionFailedOnSurvivingMastersMessage(probeFailureMessage(CONVERGENCE_SALT_STATE_SLS));

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
        assertProbeFailureCause(thrown, CONVERGENCE_SALT_STATE_SLS);
        verify(hostOrchestrator, times(1)).runOrchestratorState(cleanupParams);
        verify(hostOrchestrator, times(1)).runOrchestratorState(any());
        verify(orchestratorParamsProvider, never()).createStateParams(STACK_ID, CONVERGENCE_STATE);
    }

    @Test
    void testVerifyOnSurvivingMastersWhenBothProbesFailThenThrowsCleanupExecutionErrorWithoutRunningOrchestratorOrProbingConvergence()
            throws CloudbreakOrchestratorException {
        stubStack();
        stubSlsProbeFails(SALT_STATE_SLS);
        stubCleanupExecutionFailedMessage(REMOVED_HOST, probeFailureMessage(SALT_STATE_SLS));

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyOnSurvivingMasters(STACK_ID, Set.of(REMOVED_HOST)));

        assertThat(thrown.getMessage()).isEqualTo(CLEANUP_EXECUTION_FAILED_MESSAGE);
        assertProbeFailureCause(thrown, SALT_STATE_SLS);
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenStatePresentThenRunsOnSingleMaster() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParamsForSingleTarget(stack, MASTER_FQDN, CONVERGENCE_STATE)).thenReturn(stateParams);

        underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN);

        verify(hostOrchestrator).runOrchestratorState(stateParams);
        assertThat(convergencePillar(stateParams)).containsOnly(
                Map.entry("exclude_hosts", ""), Map.entry("timeout_sec", 300L), Map.entry("interval_sec", 5L));
        assertThat(stateParams.getStateRetryParams()).hasValueSatisfying(retryParams -> {
            // 300s / 10s = 30 polls plus margin: minutes, not the 30-minute orchestrator wall.
            assertThat(retryParams.getMaxRetry()).isEqualTo(42);
            assertThat(retryParams.getSleepTime()).isEqualTo(10_000);
        });
        verifyNoInteractions(stackService, freeIpaClientFactory);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenScriptTimesOutThenThrowsNotConvergedError() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubSingleTargetStateParams();
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_FAILED_ON_MASTER.getMessage(),
                "300", MASTER_FQDN, CONVERGENCE_LOG)).thenReturn(CONVERGENCE_FAILED_ON_MASTER_MESSAGE);
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError(CONVERGENCE_TIMEOUT_OUTPUT);
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_FAILED_ON_MASTER_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenSaltReturnsNullJidThenThrowsExecutionError() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubSingleTargetStateParams();
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException(NULL_JID_MESSAGE, ArrayListMultimap.create());
        stubConvergenceExecutionFailedOnMasterMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenNodeErrorsWithoutTimeoutMarkerThenThrowsExecutionError() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubSingleTargetStateParams();
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError("Salt minion is not responding.");
        stubConvergenceExecutionFailedOnMasterMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenCleanupTimeoutMarkerSeenThenItIsNotTreatedAsConvergenceTimeout() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, true);
        stubSingleTargetStateParams();
        CloudbreakOrchestratorFailedException cause = rewrappedWithNodeError(CLEANUP_TIMEOUT_OUTPUT);
        stubConvergenceExecutionFailedOnMasterMessage(cause.getMessage());
        doThrow(cause).when(hostOrchestrator).runOrchestratorState(any());

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE);
        assertThat(thrown.getCause()).isSameAs(cause);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenStateNotPresentThenSkips() throws CloudbreakOrchestratorException {
        stubSlsPresent(CONVERGENCE_SALT_STATE_SLS, false);

        underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN);

        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenPresenceProbeFailsThenThrowsExecutionErrorWithoutRunningState() throws CloudbreakOrchestratorException {
        stubSlsProbeFails(CONVERGENCE_SALT_STATE_SLS);
        stubConvergenceExecutionFailedOnMasterMessage(probeFailureMessage(CONVERGENCE_SALT_STATE_SLS));

        CloudbreakOrchestratorFailedException thrown = assertThrows(CloudbreakOrchestratorFailedException.class,
                () -> underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN));

        assertThat(thrown.getMessage()).isEqualTo(CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE);
        assertProbeFailureCause(thrown, CONVERGENCE_SALT_STATE_SLS);
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenPresenceProbeFailsOnceThenItIsRetriedAndStateRunsOnSingleMaster() throws CloudbreakOrchestratorException {
        doAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(0);
            try {
                return action.get();
            } catch (Retry.ActionFailedException e) {
                return action.get();
            }
        }).when(retryService).testWith1SecDelayMax3Times(any());
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt()))
                .thenThrow(new CloudbreakOrchestratorFailedException(PROBE_ERROR))
                .thenReturn(true);
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParamsForSingleTarget(stack, MASTER_FQDN, CONVERGENCE_STATE)).thenReturn(stateParams);

        underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN);

        verify(hostOrchestrator, times(2)).doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(CONVERGENCE_SALT_STATE_SLS), anyInt(), anyInt());
        verify(hostOrchestrator).runOrchestratorState(stateParams);
    }

    @Test
    void testVerifyConvergenceOnMasterWhenConvergenceDisabledThenSkipsWithoutProbingTheState() throws CloudbreakOrchestratorException {
        ReflectionTestUtils.setField(underTest, "convergenceEnabled", false);

        underTest.verifyConvergenceOnMaster(stack, MASTER_FQDN);

        verify(hostOrchestrator, never()).doesPhaseSlsExistWithTimeouts(any(), any(), anyInt(), anyInt());
        verify(hostOrchestrator, never()).runOrchestratorState(any());
        verifyNoInteractions(orchestratorParamsProvider);
    }

    private void stubCleanupFailedMessage(String removedHosts) {
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CLEANUP_FAILED.getMessage(),
                removedHosts, "600", CLEANUP_LOG)).thenReturn(CLEANUP_FAILED_MESSAGE);
    }

    private void stubCleanupExecutionFailedMessage(String removedHosts, String errorMessage) {
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CLEANUP_EXECUTION_FAILED.getMessage(),
                removedHosts, errorMessage)).thenReturn(CLEANUP_EXECUTION_FAILED_MESSAGE);
    }

    private void stubConvergenceFailedOnSurvivingMastersMessage() {
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_FAILED_ON_SURVIVING_MASTERS.getMessage(),
                "300", CONVERGENCE_LOG)).thenReturn(CONVERGENCE_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
    }

    private void stubConvergenceExecutionFailedOnSurvivingMastersMessage(String errorMessage) {
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS.getMessage(),
                errorMessage)).thenReturn(CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS_MESSAGE);
    }

    private void stubConvergenceExecutionFailedOnMasterMessage(String errorMessage) {
        when(messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_EXECUTION_FAILED_ON_MASTER.getMessage(),
                MASTER_FQDN, errorMessage)).thenReturn(CONVERGENCE_EXECUTION_FAILED_ON_MASTER_MESSAGE);
    }

    private void stubSingleTargetStateParams() {
        when(orchestratorParamsProvider.createStateParamsForSingleTarget(stack, MASTER_FQDN, CONVERGENCE_STATE))
                .thenReturn(new OrchestratorStateParams());
    }

    /**
     * Mirrors SaltOrchestrator.runOrchestratorState, which re-wraps the runner's exception without its nodesWithErrors.
     */
    private CloudbreakOrchestratorFailedException rewrappedWithNodeError(String nodeError) {
        Multimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put(NODE, nodeError);
        CloudbreakOrchestratorFailedException runnerException = new CloudbreakOrchestratorFailedException(
                "Failed: Orchestrator component went failed in 5 min(s), message: Target: [" + NODE + "]", nodesWithErrors);
        return new CloudbreakOrchestratorFailedException(runnerException.getMessage(), runnerException);
    }

    private void stubStack() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
    }

    private void stubSlsPresent(String sls, boolean present) throws CloudbreakOrchestratorException {
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(sls), anyInt(), anyInt())).thenReturn(present);
    }

    private void stubSlsProbeFails(String sls) throws CloudbreakOrchestratorException {
        when(hostOrchestrator.doesPhaseSlsExistWithTimeouts(eq(gatewayConfig), eq(sls), anyInt(), anyInt()))
                .thenThrow(new CloudbreakOrchestratorFailedException(PROBE_ERROR));
    }

    private String probeFailureMessage(String sls) {
        return String.format("Could not determine whether salt state '%s' is present on %s: %s", sls, GATEWAY_HOSTNAME, PROBE_ERROR);
    }

    private void assertProbeFailureCause(CloudbreakOrchestratorFailedException thrown, String sls) {
        assertThat(thrown.getCause()).isInstanceOf(CloudbreakOrchestratorFailedException.class);
        assertThat(thrown.getCause().getMessage()).contains(sls, GATEWAY_HOSTNAME, PROBE_ERROR);
    }

    private OrchestratorStateParams stubStateParams(String state) {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        when(orchestratorParamsProvider.createStateParams(STACK_ID, state)).thenReturn(stateParams);
        return stateParams;
    }

    private Map<String, Object> cleanupPillar(OrchestratorStateParams stateParams) {
        return pillar(stateParams, "replication_cleanup");
    }

    private Map<String, Object> convergencePillar(OrchestratorStateParams stateParams) {
        return pillar(stateParams, "replication_convergence");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pillar(OrchestratorStateParams stateParams, String key) {
        return (Map<String, Object>) ((Map<String, Object>) stateParams.getStateParams().get("freeipa")).get(key);
    }
}
