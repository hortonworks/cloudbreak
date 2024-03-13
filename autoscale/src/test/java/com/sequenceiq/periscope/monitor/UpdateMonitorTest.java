package com.sequenceiq.periscope.monitor;

import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.service.ha.PeriscopeNodeService;

@ExtendWith(MockitoExtension.class)
class UpdateMonitorTest {

    private static final String TEST_NODE_ID = randomUUID().toString();

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private PeriscopeNodeService periscopeNodeService;

    @Mock
    private NodeConfig periscopeNodeConfig;

    @InjectMocks
    private UpdateMonitor underTest;

    @Test
    void testGetMonitoredWhenLeaderIsTrueAndActivityIdsAreNotEmpty() {
        Set<Long> activityIds = LongStream.range(0, 20).boxed().collect(toSet());

        doReturn(TEST_NODE_ID).when(periscopeNodeConfig).getId();
        doReturn(Boolean.TRUE).when(periscopeNodeService).isLeader(anyString());
        doReturn(activityIds).when(scalingActivityService).findAllIdsOfSuccessfulAndInProgressScalingActivity();

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result.iterator().next().getActivityIds()).hasSameElementsAs(activityIds);
    }

    @Test
    void testGetMonitoredWhenLeaderIsNotTrue() {
        doReturn(TEST_NODE_ID).when(periscopeNodeConfig).getId();
        doReturn(Boolean.FALSE).when(periscopeNodeService).isLeader(anyString());

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).isEmpty();
        verifyNoInteractions(scalingActivityService);
    }

    @Test
    void testGetMonitoredWhenLeaderIsTrueButNoActivityIds() {
        doReturn(TEST_NODE_ID).when(periscopeNodeConfig).getId();
        doReturn(Boolean.TRUE).when(periscopeNodeService).isLeader(anyString());
        doReturn(emptySet()).when(scalingActivityService).findAllIdsOfSuccessfulAndInProgressScalingActivity();

        List<ScalingActivities> result = underTest.getMonitored();

        assertThat(result).isEmpty();
    }
}