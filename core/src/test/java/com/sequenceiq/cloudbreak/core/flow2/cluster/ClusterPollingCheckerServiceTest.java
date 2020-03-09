package com.sequenceiq.cloudbreak.core.flow2.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

class ClusterPollingCheckerServiceTest {

    private final ClusterPollingCheckerService underTest = new ClusterPollingCheckerService();

    @BeforeEach
    void setUp() {
    }

    @Test
    void nullClusterThrows() {
        assertThatThrownBy(() -> underTest.checkClusterCancelledState(null, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonCancellableReturnsNull() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        AttemptResult<Object> result = underTest.checkClusterCancelledState(cluster, false);
        assertThat(result).isNull();
    }

    @Test
    void cancellableButNotCancelledReturnsNull() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        InMemoryStateStore.putCluster(1L, PollGroup.POLLABLE);
        AttemptResult<Object> result = underTest.checkClusterCancelledState(cluster, true);
        assertThat(result).isNull();
    }

    @Test
    void cancellableAndCancelledReturnsBreak() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        InMemoryStateStore.putCluster(1L, PollGroup.CANCELLED);
        AttemptResult<Object> result = underTest.checkClusterCancelledState(cluster, true);
        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(AttemptState.BREAK);
    }
}
