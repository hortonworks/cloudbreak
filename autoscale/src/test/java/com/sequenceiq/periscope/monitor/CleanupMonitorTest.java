package com.sequenceiq.periscope.monitor;

import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.periscope.config.ScalingActivityCleanupConfig;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeService;

@ExtendWith(MockitoExtension.class)
class CleanupMonitorTest {

    private static final String TEST_NODE_ID = randomUUID().toString();

    private static final Long TEST_CLEANUP_DURATION = 24L;

    @InjectMocks
    private CleanupMonitor underTest;

    @Mock
    private PeriscopeNodeService nodeService;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private NodeConfig nodeConfig;

    @Mock
    private ScalingActivityCleanupConfig cleanupConfig;

    @Test
    void testGetMonitoredForActivityIdsWithLeader() {
        Set<Long> activityIds = LongStream.range(0, 5).boxed().collect(toSet());

        doReturn(TEST_NODE_ID).when(nodeConfig).getId();
        doReturn(Boolean.TRUE).when(nodeService).isLeader(TEST_NODE_ID);
        doReturn(TEST_CLEANUP_DURATION).when(cleanupConfig).getCleanupDurationHours();
        doReturn(activityIds).when(scalingActivityService).findAllInStatusesThatStartedBefore(anyCollection(), anyLong(), any());

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getActivityIds()).hasSameElementsAs(activityIds);
    }

    @Test
    void testGetMonitoredForNoActivityIdsWithLeader() {
        doReturn(TEST_NODE_ID).when(nodeConfig).getId();
        doReturn(Boolean.TRUE).when(nodeService).isLeader(TEST_NODE_ID);
        doReturn(TEST_CLEANUP_DURATION).when(cleanupConfig).getCleanupDurationHours();
        doReturn(emptySet()).when(scalingActivityService).findAllInStatusesThatStartedBefore(anyCollection(), anyLong(), any());

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).isEmpty();
    }

    @Test
    void testGetMonitoredForNoActivityIdsAndNotWithLeader() {
        doReturn(TEST_NODE_ID).when(nodeConfig).getId();
        doReturn(Boolean.FALSE).when(nodeService).isLeader(TEST_NODE_ID);

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).isEmpty();
        verifyNoInteractions(scalingActivityService);
    }

    @Test
    void testGetMonitoredForIdsGreaterThanLimit() {
        Set<Long> activityIds = LongStream.range(0, 500).boxed().collect(toSet());

        doReturn(TEST_NODE_ID).when(nodeConfig).getId();
        doReturn(Boolean.TRUE).when(nodeService).isLeader(TEST_NODE_ID);
        doReturn(TEST_CLEANUP_DURATION).when(cleanupConfig).getCleanupDurationHours();
        doReturn(activityIds).when(scalingActivityService).findAllInStatusesThatStartedBefore(anyCollection(), anyLong(), any());

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getActivityIds()).hasSize(200);
    }
}