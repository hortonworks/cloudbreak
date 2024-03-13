package com.sequenceiq.periscope.service.ha;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.evaluator.CronTimeEvaluator;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.DateTimeService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;

@ExtendWith(MockitoExtension.class)
class LeaderElectionServiceTest {

    @InjectMocks
    private LeaderElectionService underTest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private NodeConfig periscopeNodeConfig;

    @Mock
    private PeriscopeNodeRepository periscopeNodeRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private Clock clock;

    @Mock
    private TransactionService transactionService;

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

    @BeforeEach
    void setup() {
        lenient().when(periscopeNodeConfig.isNodeIdSpecified()).thenReturn(true);
        lenient().when(periscopeNodeConfig.getId()).thenReturn("nodeid");
        lenient().when(timerFactory.get()).thenReturn(timer);
        when(clock.getCurrentTimeMillis()).thenReturn(5000L);
        lenient().when(applicationContext.getBean(eq("CronTimeEvaluator"), eq(CronTimeEvaluator.class))).thenReturn(cronTimeEvaluator);
        ReflectionTestUtils.setField(underTest, "heartbeatThresholdRate", 70000);
    }

    @Test
    void testElectionNotNeeded() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(1L);

        underTest.leaderElection();

        verify(transactionService, times(0)).required(any(Supplier.class));
        verify(timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void testElectionNeededAndSuccess() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);

        underTest.leaderElection();

        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(timer, times(1)).cancel();
        verify(timer, times(1)).purge();
        verify(timerFactory, times(1)).get();
        verify(timer, times(1)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void testElectionNeededAndFails() throws TransactionExecutionException {
        when(periscopeNodeRepository.countByLeaderIsTrueAndLastUpdatedIsGreaterThan(anyLong())).thenReturn(0L);
        when(transactionService.required(any(Supplier.class))).thenThrow(new TransactionExecutionException("Persisting went wrong", new RuntimeException()));

        underTest.leaderElection();

        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(timer, times(1)).cancel();
        verify(timer, times(1)).purge();
        verify(timerFactory, times(1)).get();
        verify(timer, times(0)).schedule(any(TimerTask.class), anyLong(), anyLong());
    }

    @Test
    void testReallocateOrphanClustersIsNotLeader() throws TransactionExecutionException {
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
    void testReallocateOrphanClustersIsLeader() throws TransactionExecutionException {
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
    void testIsExecutionOfMissedTimeBasedAlertsNeededNoPeriscopeNodeId() {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setPeriscopeNodeId(null);

        boolean needed = ReflectionTestUtils.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        assertFalse(needed);
    }

    @Test
    void testIsExecutionOfMissedTimeBasedAlertsNeededNoAutoscaling() {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setAutoscalingEnabled(false);

        boolean needed = ReflectionTestUtils.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        assertFalse(needed);
    }

    @Test
    void testIsExecutionOfMissedTimeBasedAlertsNeededLastScalingHappend() {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setCoolDown(1);

        boolean needed = ReflectionTestUtils.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        assertFalse(needed);
    }

    @Test
    void testIsExecutionOfMissedTimeBasedAlertsNeededNoAlerts() {
        Cluster cluster = getValidIsMissedNeeded();
        cluster.setTimeAlerts(null);

        boolean needed = ReflectionTestUtils.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        assertFalse(needed);
    }

    @Test
    void testIsExecutionOfMissedTimeBasedAlertsNeededTrue() {
        Cluster cluster = getValidIsMissedNeeded();

        boolean needed = ReflectionTestUtils.invokeMethod(underTest, "isExecutionOfMissedTimeBasedAlertsNeeded", cluster);

        assertTrue(needed);
    }

    @Test
    void testExecuteMissedTimeBasedAlertsNotNeedLastEvalLessThanRewind() {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(2);
        cluster.setLastEvaluated(4900L);
        cluster.setTimeAlerts(Collections.singleton(new TimeAlert()));

        ReflectionTestUtils.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(0)).getBean(anyString(), eq(CronTimeEvaluator.class));
        verify(cronTimeEvaluator, times(0)).publishIfNeeded(any(Map.class));
    }

    @Test
    void testExecuteMissedTimeBasedAlertsNotNeedCooldownLessThanRewind() {
        Cluster cluster = new Cluster();
        cluster.setCoolDown(0);
        cluster.setLastEvaluated(1L);
        cluster.setTimeAlerts(Collections.singleton(new TimeAlert()));

        ReflectionTestUtils.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(0)).getBean(anyString(), eq(CronTimeEvaluator.class));
        verify(cronTimeEvaluator, times(0)).publishIfNeeded(any(Map.class));
    }

    @Test
    void testExecuteMissedTimeBasedAlertsNeed() {
        ZonedDateTime now = ZonedDateTime.now();
        when(dateTimeService.getDefaultZonedDateTime()).thenReturn(now);
        when(dateTimeService.getNextSecound(any())).thenReturn(now);
        Cluster cluster = new Cluster();
        cluster.setCoolDown(5);
        cluster.setLastEvaluated(2900L);
        TimeAlert timeAlert = new TimeAlert();
        cluster.setTimeAlerts(Collections.singleton(timeAlert));

        ReflectionTestUtils.invokeMethod(underTest, "executeMissedTimeBasedAlerts", cluster);

        verify(applicationContext, times(1)).getBean(anyString(), eq(CronTimeEvaluator.class));

        Map<TimeAlert, ZonedDateTime> expectedAlerts = new LinkedHashMap<>();
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
