package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorInProgressException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTerminateException;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltErrorResolver;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunningJobsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
class SaltJobIdTrackerTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltConnector saltConnector;

    @Captor
    private ArgumentCaptor<Target<String>> targetCaptor;

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @Test
    void callWithNotStarted() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(SaltJobRunner.class);
        when(saltStateService.jobIsRunning(any(), any())).thenReturn(true);
        RunningJobsResponse jobsResponse = new RunningJobsResponse();
        jobsResponse.setResult(List.of());
        when(saltStateService.getRunningJobs(saltConnector)).thenReturn(jobsResponse);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenReturn(JobState.NOT_STARTED, JobState.IN_PROGRESS);
        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);

        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("Target:", "10.0.0.1", "10.0.0.2", "10.0.0.3")
                .isInstanceOf(CloudbreakOrchestratorInProgressException.class);
        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
        verify(saltJobRunner, times(3)).getJobState();
        verify(saltJobRunner, times(2)).setJobState(JobState.IN_PROGRESS);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @Test
    void callWithNotStartedAndJobIsRunningFailsThenJobStateIsSet() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(SaltJobRunner.class);
        when(saltStateService.jobIsRunning(any(), any())).thenThrow(new RuntimeException());
        RunningJobsResponse jobsResponse = new RunningJobsResponse();
        jobsResponse.setResult(List.of());
        when(saltStateService.getRunningJobs(saltConnector)).thenReturn(jobsResponse);

        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenReturn(JobState.NOT_STARTED, JobState.IN_PROGRESS);
        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);

        assertThatThrownBy(saltJobIdTracker::call)
                .isInstanceOf(RuntimeException.class);
        verify(saltJobRunner).setJobState(JobState.IN_PROGRESS);
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @Test
    void callWithNotStartedWithAlreadyRunning() throws Exception {
        SaltJobRunner saltJobRunner = mock(SaltJobRunner.class);
        RunningJobsResponse jobsResponse = new RunningJobsResponse();
        jobsResponse.setResult(List.of(Map.of("runningJob", Map.of())));
        when(saltStateService.getRunningJobs(saltConnector)).thenReturn(jobsResponse);

        when(saltJobRunner.getJobState()).thenReturn(JobState.NOT_STARTED);
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("There are running job(s) with id:", "runningJob")
                .isInstanceOf(CloudbreakOrchestratorInProgressException.class);
        verify(saltJobRunner, times(1)).getJobState();
    }

    @Test
    void callWithFailed() {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);
        saltJobRunner.setJobState(JobState.FAILED);
        Multimap<String, String> multimap = ArrayListMultimap.create();
        multimap.put("10.0.0.1", "some error");
        when(saltJobRunner.getNodesWithError()).thenReturn(multimap);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner, false);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("Target:", "10.0.0.1", "10.0.0.3", "Node: 10.0.0.1 Error(s): some error")
                .isInstanceOf(CloudbreakOrchestratorTerminateException.class);
    }

    private void checkTargets(Set<String> targets, List<Target<String>> allValues) {
        for (Target<String> allValue : allValues) {
            for (String target : targets) {
                assertThat(allValue.getTarget(), containsString(target));
            }
        }
    }

    @Test
    void callWithInProgressAndJobIsRunning() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);

        when(saltStateService.jobIsRunning(any(), any())).thenReturn(true);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("Target:", "10.0.0.1", "10.0.0.2", "10.0.0.3")
                .isInstanceOf(CloudbreakOrchestratorInProgressException.class);

        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    void callWithInProgressAndJobIsFinished() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        SaltErrorResolver saltErrorResolver = mock(SaltErrorResolver.class);
        when(saltConnector.getSaltErrorResolver()).thenReturn(saltErrorResolver);

        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");

        when(saltStateService.jobIsRunning(any(), any())).thenReturn(false);

        Multimap<String, Map<String, String>> missingNodesWithReason = ArrayListMultimap.create();
        Multimap<String, String> missingNodesWithResolvedReason = ArrayListMultimap.create();
        when(saltStateService.jidInfo(any(), any(), any())).thenReturn(missingNodesWithReason);
        when(saltErrorResolver.resolveErrorMessages(missingNodesWithReason)).thenReturn(missingNodesWithResolvedReason);

        SaltJobIdTracker underTest = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertTrue(underTest.call());

        assertEquals(JobState.FINISHED, saltJobRunner.getJobState());

        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    void callWithInProgressAndMissingNodes() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());
        when(saltJobRunner.getNodesWithError()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setNodesWithError(any());
        SaltErrorResolver saltErrorResolver = mock(SaltErrorResolver.class);
        when(saltConnector.getSaltErrorResolver()).thenReturn(saltErrorResolver);
        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);

        Multimap<String, Map<String, String>> missingNodesWithReason = ArrayListMultimap.create();
        Multimap<String, String> missingNodesWithResolvedReason = ArrayListMultimap.create();
        String missingMachine = "10.0.0.1";
        missingNodesWithReason.put(missingMachine, Collections.singletonMap("Name", "some-script.sh"));
        missingNodesWithResolvedReason.put(missingMachine, "Failed to execute: {Name=some-script.sh}");
        when(saltErrorResolver.resolveErrorMessages(missingNodesWithReason)).thenReturn(missingNodesWithResolvedReason);

        when(saltStateService.jobIsRunning(any(), any())).thenReturn(false);
        when(saltStateService.jidInfo(any(SaltConnector.class), anyString(), any())).thenReturn(missingNodesWithReason);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("Target:", "10.0.0.1", "10.0.0.2", "10.0.0.3",
                        "Node: 10.0.0.1 Error(s): Failed to execute: {Name=some-script.sh}")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);

        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    void callWithMissingNodesUsingStderrFailures() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());
        when(saltJobRunner.getNodesWithError()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setNodesWithError(any());
        SaltErrorResolver saltErrorResolver = mock(SaltErrorResolver.class);
        when(saltConnector.getSaltErrorResolver()).thenReturn(saltErrorResolver);
        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);

        Multimap<String, Map<String, String>> missingNodesWithReason = ArrayListMultimap.create();
        Multimap<String, String> missingNodesWithResolvedReason = ArrayListMultimap.create();
        String missingMachine = "10.0.0.1";
        missingNodesWithReason.put(missingMachine, Map.of("Name", "/opt/salt/scripts/backup_db.sh", "Stderr", "Could not create backup"));
        missingNodesWithResolvedReason.put(missingMachine, "Could not create backup");
        when(saltErrorResolver.resolveErrorMessages(missingNodesWithReason)).thenReturn(missingNodesWithResolvedReason);

        when(saltStateService.jobIsRunning(any(), any())).thenReturn(false);
        when(saltStateService.jidInfo(any(SaltConnector.class), anyString(), any())).thenReturn(missingNodesWithReason);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll("Target:", "10.0.0.1", "10.0.0.2", "10.0.0.3",
                        "Node: 10.0.0.1 Error(s): Could not create backup")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);

        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    void callWithInProgressAndMissingNodesAndNoRetryOnFail() {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        Multimap<String, String> missingNodesWithReason = ArrayListMultimap.create();
        String missingMachine = "10.0.0.1";
        String errorMessage = "Name: some-script.sh";
        missingNodesWithReason.put(missingMachine, errorMessage);
        when(saltJobRunner.getNodesWithError()).thenReturn(missingNodesWithReason);
        saltJobRunner.setJobState(JobState.FAILED);

        Set<String> targets = Sets.newHashSet("10.0.0.1", "10.0.0.2", "10.0.0.3");
        when(saltJobRunner.getTargetHostnames()).thenReturn(targets);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner, false);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContainingAll(missingMachine, errorMessage)
                .isInstanceOf(CloudbreakOrchestratorTerminateException.class);

        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    void callWithNotStartedAndSlsWithError() throws Exception {
        String jobId = "1";
        SaltJobRunner saltJobRunner = mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());
        when(saltJobRunner.getNodesWithError()).thenCallRealMethod();
        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        saltJobRunner.setJobState(JobState.NOT_STARTED);

        Set<String> targets = Sets.newHashSet("10.0.0.1", "10.0.0.2", "10.0.0.3");

        when(saltStateService.jobIsRunning(any(), any())).thenReturn(false);
        when(saltStateService.jidInfo(any(SaltConnector.class), eq(jobId), any()))
                .thenThrow(new RuntimeException("Salt execution went wrong: saltErrorDetails"));
        RunningJobsResponse jobsResponse = new RunningJobsResponse();
        jobsResponse.setResult(List.of());
        when(saltStateService.getRunningJobs(saltConnector)).thenReturn(jobsResponse);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltStateService, saltConnector, saltJobRunner);
        assertThatThrownBy(saltJobIdTracker::call)
                .hasMessageContaining("Salt execution went wrong: saltErrorDetails")
                .hasMessageNotContaining("Exception")
                .isInstanceOf(CloudbreakOrchestratorFailedException.class);

        verify(saltStateService).jobIsRunning(any(), eq(jobId));
        checkTargets(targets, targetCaptor.getAllValues());
    }
}
