package com.sequenceiq.periscope.monitor;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.ClusterService;

@ExtendWith(MockitoExtension.class)
class ClusterStatusMonitorTest {

    private static final Long TEST_CLUSTER_ID = 1L;

    private static final String TEST_PERISCOPE_NODE_CONFIG = "periscope-node-id";

    @InjectMocks
    private ClusterStatusMonitor underTest;

    @Mock
    private ClusterService clusterService;

    @Test
    void testGetMonitoredForAutoscalingEnabled() {
        ClusterStatusMonitor spy = spy(underTest);
        NodeConfig periscopeNodeConfig = mock(NodeConfig.class);

        when(spy.getClusterService()).thenReturn(clusterService);
        when(spy.getPeriscopeNodeConfig()).thenReturn(periscopeNodeConfig);
        when(clusterService.findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(any(StackType.class), anyString(), anyBoolean()))
                .thenReturn(List.of(TEST_CLUSTER_ID));
        when(periscopeNodeConfig.getId()).thenReturn(TEST_PERISCOPE_NODE_CONFIG);

        List<Cluster> result = spy.getMonitored();

        assertThat(result).hasSize(1);
        verify(clusterService).findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(any(StackType.class), anyString(), anyBoolean());
    }
}