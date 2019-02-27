package com.sequenceiq.periscope.service.ha;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.internal.WhiteboxImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.DateTimeService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.StackCollectorService;

@RunWith(MockitoJUnitRunner.class)
public class LeaderElectionServiceTest {

    @InjectMocks
    private LeaderElectionService underTest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private PeriscopeNodeConfig periscopeNodeConfig;

    @Mock
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private Clock clock;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackCollectorService stackCollectorService;

    @Mock
    private Timer timer;

    @Mock
    private Supplier<Timer> timerFactory;

    @Mock
    private DateTimeService dateTimeService;

    @Mock
    private CronTimeEvaluator cronTimeEvaluator;

    @Mock
    private PeriscopeMetricService metricService;

    @Before
    public void init() {
        when(periscopeNodeConfig.isNodeIdSpecified()).thenReturn(true);
        when(periscopeNodeConfig.getId()).thenReturn("nodeid");
        when(timerFactory.get()).thenReturn(timer);
        when(clock.getCurrentTimeMillis()).thenReturn(5000L);
        when(applicationContext.getBean(eq("CronTimeEvaluator"), eq(CronTimeEvaluator.class))).thenReturn(cronTimeEvaluator);
        ReflectionTestUtils.setField(underTest, "heartbeatThresholdRate", 70000);
    }

    @Test
    public void testElectionNotNeeded() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(1L);

        underTest.leaderElection();

        verify(transactionService, times(0)).required(any(Supplier.class));
        verify(timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    public void testElectionNeededAndSuccess() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);

        underTest.leaderElection();

        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(timer, times(1)).cancel();
        verify(timer, times(1)).purge();
        verify(timerFactory, times(1)).get();
        verify(timer, times(1)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    public void testElectionNeededAndFails() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);
        when(transactionService.required(any())).thenThrow(new TransactionExecutionException("Persisting went wrong", new RuntimeException()));

        underTest.leaderElection();

        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(timer, times(1)).cancel();
        verify(timer, times(1)).purge();
        verify(timerFactory, times(1)).get();
        verify(timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    public void testReallocateOrphanClustersIsNotLeader() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);
        SpyTimer spyTimer = new SpyTimer();
        when(timerFactory.get()).thenReturn(spyTimer);
        when(periscopeNodeRepository.findAllByLastUpdatedIsGreaterThan(anyLong())).thenReturn(Collections.singletonList(new PeriscopeNode("othernodeid")));

        underTest.leaderElection();

        spyTimer.lastTask.run();

        verify(transactionService, times(2)).required(any(Supplier.class));
        verify(clusterRepository, times(0)).findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(any(List.class));
    }

    @Test
    public void testReallocateOrphanClustersIsLeader() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);
        SpyTimer spyTimer = new SpyTimer();
        when(timerFactory.get()).thenReturn(spyTimer);
        PeriscopeNode leader = new PeriscopeNode("nodeid");
        leader.setLeader(true);
        when(periscopeNodeRepository.findAllByLastUpdatedIsGreaterThan(anyLong())).thenReturn(Collections.singletonList(leader));
        when(clusterRepository.findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(any())).thenReturn(Collections.singletonList(new Cluster()));

        underTest.leaderElection();

        spyTimer.lastTask.run();

        verify(transactionService, times(2)).required(any(Supplier.class));
        verify(clusterRepository, times(1)).findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(any(List.class));
        verify(clusterRepository, times(1)).saveAll(any(List.class));
    }

    @Test
    public void testIsExecutionOfMissedTimeBasedAlertsNeededNoPeriscopeNodeId() throws Exception {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setPeriscopeNodeId(null);

        boolean needed = WhiteboxImpl.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        Assert.assertFalse(needed);
    }

    @Test
    public void testIsExecutionOfMissedTimeBasedAlertsNeededNoAutoscaling() throws Exception {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setAutoscalingEnabled(false);

        boolean needed = WhiteboxImpl.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        Assert.assertFalse(needed);
    }

    @Test
    public void testIsExecutionOfMissedTimeBasedAlertsNeededLastScalingHappend() throws Exception {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setCoolDown(1);

        boolean needed = WhiteboxImpl.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        Assert.assertFalse(needed);
    }

    @Test
    public void testIsExecutionOfMissedTimeBasedAlertsNeededNoAlerts() throws Exception {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setTimeAlerts(null);

        boolean needed = WhiteboxImpl.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        Assert.assertFalse(needed);
    }

    @Test
    public void testIsExecutionOfMissedTimeBasedAlertsNeededTrue() throws Exception {
        Cluster cluster = getValidIsMissedNeeded();

        boolean needed = WhiteboxImpl.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        Assert.assertTrue(needed);
    }

    @Test
    public void testExecuteMissedTimeBasedAlertsNotNeedLastEvalLessThanRewind() throws Exception {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(2);
        cluster.setLastEvaluated(4900L);
        cluster.setTimeAlerts(Collections.singleton(new TimeAlert()));

        WhiteboxImpl.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(0)).getBean(anyString(), eq(CronTimeEvaluator.class));
        verify(cronTimeEvaluator, times(0)).publishIfNeeded(any(Map.class));
    }

    @Test
    public void testExecuteMissedTimeBasedAlertsNotNeedCooldownLessThanRewind() throws Exception {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(0);
        cluster.setLastEvaluated(1L);
        cluster.setTimeAlerts(Collections.singleton(new TimeAlert()));

        WhiteboxImpl.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(0)).getBean(anyString(), eq(CronTimeEvaluator.class));
        verify(cronTimeEvaluator, times(0)).publishIfNeeded(any(Map.class));
    }

    @Test
    public void testExecuteMissedTimeBasedAlertsNeed() throws Exception {
        ZonedDateTime now = ZonedDateTime.now();
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(now);
        when(dateTimeService.getNextSecound(any())).thenReturn(now);
        Cluster cluster = new Cluster();
        cluster.setCoolDown(5);
        cluster.setLastEvaluated(2900L);
        TimeAlert timeAlert = new TimeAlert();
        cluster.setTimeAlerts(Collections.singleton(timeAlert));

        WhiteboxImpl.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(1)).getBean(anyString(), eq(CronTimeEvaluator.class));

        Map<TimeAlert, ZonedDateTime> expectedAlerts = new LinkedHashMap<>();
        expectedAlerts.put(timeAlert, now);
        expectedAlerts.put(timeAlert, now);
        verify(cronTimeEvaluator, times(1)).publishIfNeeded(eq(expectedAlerts));
    }

    private Cluster getValidIsMissedNeeded() {
        Cluster cluster = new Cluster();
        cluster.setPeriscopeNodeId("");
        cluster.setAutoscalingEnabled(true);
        cluster.setCoolDown(0);
        cluster.setLastScalingActivity(4900L);
        cluster.setLastEvaluated(1L);
        cluster.setTimeAlerts(Collections.singleton(null));
        return cluster;
    }

    static class SpyTimer extends Timer {

        private TimerTask lastTask;

        @Override
        public void schedule(TimerTask task, long initDelay, long delay) {
            lastTask = task;
        }
    }
}
