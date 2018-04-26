package com.sequenceiq.periscope.service.ha;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeNode;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;
import com.sequenceiq.periscope.service.StackCollectorService;

@RunWith(MockitoJUnitRunner.class)
public class LeaderElectionServiceTest {

    @InjectMocks
    private LeaderElectionService underTest;

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

    @Before
    public void init() {
        when(periscopeNodeConfig.isNodeIdSpecified()).thenReturn(true);
        when(periscopeNodeConfig.getId()).thenReturn("nodeid");
        when(timerFactory.get()).thenReturn(timer);
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

        verify(transactionService, times(1)).required(any(Supplier.class));
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

        verify(transactionService, times(1)).required(any(Supplier.class));
        verify(clusterRepository, times(1)).findAllByPeriscopeNodeIdNotInOrPeriscopeNodeIdIsNull(any(List.class));
        verify(clusterRepository, times(1)).save(any(List.class));
    }

    static class SpyTimer extends Timer {

        private TimerTask lastTask;

        @Override
        public void schedule(TimerTask task, long initDelay, long delay) {
            lastTask = task;
        }
    }
}
