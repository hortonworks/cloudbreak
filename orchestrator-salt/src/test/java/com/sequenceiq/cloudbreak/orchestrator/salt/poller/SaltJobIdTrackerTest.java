package com.sequenceiq.cloudbreak.orchestrator.salt.poller;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobId;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JobState;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SaltStates.class)
public class SaltJobIdTrackerTest {


    @Captor
    private ArgumentCaptor<Target<String>> targetCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void callWithNotStarted() throws Exception {
        String jobId = "1";
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        SaltJobRunner saltJobRunner = Mockito.mock(SaltJobRunner.class);
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.jobIsRunning(any(), any(), any())).thenReturn(true);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTarget()).thenReturn(targets);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenReturn(JobState.NOT_STARTED, JobState.IN_PROGRESS);
        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, saltJobRunner);
        try {
            saltJobIdTracker.call();
            fail("should throw exception");
        } catch (CloudbreakOrchestratorFailedException e) {
            assertThat(e.getMessage(), both(containsString("jobId='" + jobId + "'")).and(containsString("is running")));
        }
        PowerMockito.verifyStatic();
        SaltStates.jobIsRunning(any(), eq(jobId), targetCaptor.capture());
        checkTargets(targets, targetCaptor.getAllValues());
        verify(saltJobRunner, times(2)).getJobState();
    }

    @Test
    public void callWithFailed() throws Exception {
        String jobId = "1";
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);
        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.jobIsRunning(any(), any(), any())).thenReturn(true);

        SaltJobRunner saltJobRunner = Mockito.mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        saltJobRunner.setJobState(JobState.FAILED);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, saltJobRunner);
        try {
            saltJobIdTracker.call();
            fail("should throw exception");
        } catch (CloudbreakOrchestratorFailedException e) {
            assertThat(e.getMessage(), both(containsString("jobId='" + jobId + "'")).and(containsString("is running")));
        }
        PowerMockito.verifyStatic();
        SaltStates.jobIsRunning(any(), eq(jobId), any());
    }

    private void checkTargets(Set<String> targets, List<Target<String>> allValues) {
        for (Target<String> allValue : allValues) {
            for (String target : targets) {
                assertThat(allValue.getTarget(), containsString(target));
            }
        }
    }

    @Test
    public void callWithInProgressAndJobIsRunning() throws Exception {
        String jobId = "1";
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        SaltJobRunner saltJobRunner = Mockito.mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTarget()).thenReturn(targets);

        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.jobIsRunning(any(), any(), any())).thenReturn(true);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, saltJobRunner);
        try {
            saltJobIdTracker.call();
        } catch (CloudbreakOrchestratorFailedException e) {
            assertThat(e.getMessage(), both(containsString("jobId='" + jobId + "'")).and(containsString("is running")));
        }

        PowerMockito.verifyStatic();
        SaltStates.jobIsRunning(any(), eq(jobId), targetCaptor.capture());
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    public void callWithInProgressAndJobIsFinished() throws Exception {
        String jobId = "1";
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        SaltJobRunner saltJobRunner = Mockito.mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTarget()).thenReturn(targets);

        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.jobIsRunning(any(), any(), any())).thenReturn(false);

        Multimap<String, String> missingNodesWithReason = ArrayListMultimap.create();
        PowerMockito.when(SaltStates.jidInfo(any(), any(), any(), any())).thenReturn(missingNodesWithReason);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, saltJobRunner);
        assertTrue(saltJobIdTracker.call());

        assertEquals(JobState.FINISHED, saltJobRunner.getJobState());

        PowerMockito.verifyStatic();
        SaltStates.jobIsRunning(any(), eq(jobId), targetCaptor.capture());
        checkTargets(targets, targetCaptor.getAllValues());
    }

    @Test
    public void callWithInProgressAndMissingNodes() throws Exception {
        String jobId = "1";
        SaltConnector saltConnector = Mockito.mock(SaltConnector.class);

        SaltJobRunner saltJobRunner = Mockito.mock(BaseSaltJobRunner.class);
        when(saltJobRunner.getJid()).thenReturn(JobId.jobId(jobId));
        when(saltJobRunner.getJobState()).thenCallRealMethod();
        doCallRealMethod().when(saltJobRunner).setJobState(any());

        when(saltJobRunner.submit(any(SaltConnector.class))).thenReturn(jobId);
        saltJobRunner.setJobState(JobState.IN_PROGRESS);

        Set<String> targets = new HashSet<>();
        targets.add("10.0.0.1");
        targets.add("10.0.0.2");
        targets.add("10.0.0.3");
        when(saltJobRunner.getTarget()).thenReturn(targets);

        PowerMockito.mockStatic(SaltStates.class);
        PowerMockito.when(SaltStates.jobIsRunning(any(), any(), any())).thenReturn(false);

        Multimap<String, String> missingNodesWithReason = ArrayListMultimap.create();
        String missingMachine = "10.0.0.1";
        String errorMessage = "error happened";
        missingNodesWithReason.put(missingMachine, errorMessage);
        PowerMockito.when(SaltStates.jidInfo(any(), any(), any(), any())).thenReturn(missingNodesWithReason);

        SaltJobIdTracker saltJobIdTracker = new SaltJobIdTracker(saltConnector, saltJobRunner);
        try {
            saltJobIdTracker.call();
            fail("should throw exception");
        } catch (CloudbreakOrchestratorFailedException e) {
            assertThat(e.getMessage(), both(containsString(missingMachine)).and(containsString(errorMessage)));
        }

        PowerMockito.verifyStatic();
        SaltStates.jobIsRunning(any(), eq(jobId), targetCaptor.capture());
        checkTargets(targets, targetCaptor.getAllValues());
    }

}