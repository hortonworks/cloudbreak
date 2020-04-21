package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.api.model.AdjustmentType.EXACT;
import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static com.sequenceiq.periscope.api.model.AdjustmentType.PERCENTAGE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RejectedThreadService;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
public class ScalingHandlerTest {

    private static final long AUTOSCALE_CLUSTER_ID = 101L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    private static final Integer TEST_HOSTGROUP_MAX_SIZE = 200;

    private static final Integer TEST_HOSTGROUP_MIN_SIZE = 0;

    @InjectMocks
    private ScalingHandler underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private RejectedThreadService rejectedThreadService;

    @Mock
    private ExecutorService executorService;

    @Mock
    private ApplicationContext applicationContext;

    private ScalingPolicy scalingPolicyMock = mock(ScalingPolicy.class);

    private ScalingEvent scalingEventMock = mock(ScalingEvent.class);

    private Runnable runnableMock = mock(Runnable.class);

    private StackV4Response stackV4ResponseMock = mock(StackV4Response.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnApplicationEventWhenLoadAlertDecommissionNodes() {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        Cluster cluster = getARunningCluster();

        int hostGroupNodeCount = 10;
        List nodeIds = List.of("nodeId1", "nodeId2", "nodeId3");
        int expectedNodeCount = hostGroupNodeCount - nodeIds.size();

        when(scalingEventMock.getAlert()).thenReturn(loadAlertMock);
        when(loadAlertMock.getCluster()).thenReturn(cluster);
        when(loadAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(scalingPolicyMock.getAdjustmentType()).thenReturn(LOAD_BASED);
        when(scalingEventMock.getHostGroupNodeCount()).thenReturn(Optional.of(hostGroupNodeCount));
        when(scalingEventMock.getDecommissionNodeIds()).thenReturn(nodeIds);
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                hostGroupNodeCount, expectedNodeCount, nodeIds)).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).save(cluster);
        verify(executorService).submit(runnableMock);
    }

    @Test
    public void testOnApplicationEventWhenNoScaling() {
        Cluster cluster = getARunningCluster();
        String testHostGroup = "compute";
        TimeAlert timeAlertMock = mock(TimeAlert.class);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(timeAlertMock.getCluster()).thenReturn(cluster);
        when(timeAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);

        when(scalingPolicyMock.getAdjustmentType()).thenReturn(EXACT);
        when(scalingPolicyMock.getHostGroup()).thenReturn(testHostGroup);
        when(scalingPolicyMock.getScalingAdjustment()).thenReturn(2);

        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4ResponseMock);
        when(stackResponseUtils.getNodeCountForHostGroup(stackV4ResponseMock, testHostGroup))
                .thenReturn(2);

        underTest.onApplicationEvent(new ScalingEvent(timeAlertMock));

        verify(clusterService, never()).save(cluster);
        verify(applicationContext, never()).getBean("ScalingRequest");
    }

    public static Stream<Arguments> dataLoadScaling() {
        return Stream.of(
                //TestCase, AdjustmentType, CurrentHostGroupCount,ScalingAdjustment,ExpectedScalingCount
                Arguments.of("LOAD_SCALEUP_WITHIN_MAX_LIMIT",  2,  5, 7),
                Arguments.of("LOAD_SCALEUP_WITHIN_MAX_LIMIT", 10, 40, 50),
                Arguments.of("LOAD_SCALEUP_WITHIN_MAX_LIMIT", 10, 15, 25),
                Arguments.of("LOAD_SCALEUP_WITHIN_MAX_LIMIT", 10, 40, 50),
                Arguments.of("LOAD_SCALEUP_BEYOND_MAX_LIMIT",  4, 202, TEST_HOSTGROUP_MAX_SIZE)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, scalingAdjustment ={2}, expectedScalingCount={3} ")
    @MethodSource("dataLoadScaling")
    public void testLoadBasedScaling(String testType, int currentHostGroupCount,
            int scalingAdjustment, int expectedScalingCount) {
        LoadAlert loadAlertMock = mock(LoadAlert.class);
        validateClusterScaling(loadAlertMock, LOAD_BASED, currentHostGroupCount, scalingAdjustment, expectedScalingCount);
    }

    public static Stream<Arguments> dataTmeScaling() {
        return Stream.of(
                            //TestCase, AdjustmentType, CurrentHostGroupCount,ScalingAdjustment,ExpectedScalingCount
                Arguments.of("TIME_SCALEUP_NODE_COUNT_WITHIN_MAX_LIMIT", NODE_COUNT, 2, 25, 27),
                Arguments.of("TIME_SCALEUP_NODE_COUNT_AT_MAX_LIMIT", NODE_COUNT, 2, 198, TEST_HOSTGROUP_MAX_SIZE),
                Arguments.of("TIME_SCALEUP_NODE_COUNT_BEYOND_MAX_LIMIT", NODE_COUNT, 2,  1000, TEST_HOSTGROUP_MAX_SIZE),

                Arguments.of("TIME_SCALEUP_PERCENTAGE_WITHIN_MAX_LIMIT", PERCENTAGE, 2, 50, 3),
                Arguments.of("TIME_SCALEUP_PERCENTAGE_WITHIN_MAX_LIMIT", PERCENTAGE, 2, 100, 4),
                Arguments.of("TIME_SCALEUP_PERCENTAGE_BEYOND_MAX_LIMIT", PERCENTAGE, 101, 1000, TEST_HOSTGROUP_MAX_SIZE),

                Arguments.of("TIME_SCALEUP_EXACT_WITHIN_MAX_LIMIT", EXACT,  2, 10, 10),
                Arguments.of("TIME_SCALEUP_EXACT_WITHIN_MAX_LIMIT", EXACT, 12, 24, 24),
                Arguments.of("TIME_SCALEUP_EXACT_BEYOND_MAX_LIMIT", EXACT, 40, 1000, TEST_HOSTGROUP_MAX_SIZE),

                Arguments.of("TIME_SCALEDOWN_NODE_COUNT_BEYOND_MIN_LIMIT", NODE_COUNT, 2, -12, TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("TIME_SCALEDOWN_PERCENTAGE_BEYOND_MIN_LIMIT", PERCENTAGE, 2, -100, TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("TIME_SCALEDOWN_EXACT_BEYOND_MIN_LIMIT", EXACT, 2, 0, TEST_HOSTGROUP_MIN_SIZE),

                Arguments.of("TIME_SCALEDOWN_NODE_COUNT_WITHIN_MIN_LIMIT", NODE_COUNT, 10, -2, 8),
                Arguments.of("TIME_SCALEDOWN_PERCENTAGE_WITHIN_MIN_LIMIT", PERCENTAGE, 10, -50, 5),
                Arguments.of("TIME_SCALEDOWN_EXACT_WITHIN_MIN_LIMIT", EXACT, 10, 6, 6)
                );
    }

    @ParameterizedTest(name = "{0}: With AdjustmentType{1}, currentHostGroupCount={2}, scalingAdjustment ={3}, expectedScalingCount={4} ")
    @MethodSource("dataTmeScaling")
    public void testTimeBasedScaling(String testType, AdjustmentType adjustmentType, int currentHostGroupCount,
            int scalingAdjustment, int expectedScalingCount) {
        TimeAlert timeAlertMock = mock(TimeAlert.class);
        validateClusterScaling(timeAlertMock, adjustmentType, currentHostGroupCount, scalingAdjustment, expectedScalingCount);
    }

    private void validateClusterScaling(BaseAlert baseAlertMock, AdjustmentType adjustmentType,
            int currentHostGroupCount, int scalingAdjument, int expectedScaleUpCount) {
        MockitoAnnotations.initMocks(this);
        Cluster cluster = getARunningCluster();
        String testHostGroup = "compute";

        when(scalingEventMock.getAlert()).thenReturn(baseAlertMock);
        if (LOAD_BASED.equals(adjustmentType)) {
            when(scalingEventMock.getHostGroupNodeCount()).thenReturn(Optional.of(currentHostGroupCount));
            when(scalingEventMock.getScalingNodeCount()).thenReturn(Optional.of(scalingAdjument));
        } else {
            when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4ResponseMock);
            when(stackResponseUtils.getNodeCountForHostGroup(stackV4ResponseMock, testHostGroup))
                    .thenReturn(currentHostGroupCount);
        }
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(baseAlertMock.getCluster()).thenReturn(cluster);
        when(baseAlertMock.getScalingPolicy()).thenReturn(scalingPolicyMock);

        when(scalingPolicyMock.getAdjustmentType()).thenReturn(adjustmentType);
        when(scalingPolicyMock.getHostGroup()).thenReturn(testHostGroup);
        when(scalingPolicyMock.getScalingAdjustment()).thenReturn(scalingAdjument);
        when(scalingEventMock.getDecommissionNodeIds()).thenCallRealMethod();
        when(applicationContext.getBean("ScalingRequest", cluster, scalingPolicyMock,
                currentHostGroupCount, expectedScaleUpCount, List.of())).thenReturn(runnableMock);

        underTest.onApplicationEvent(scalingEventMock);

        verify(rejectedThreadService).remove(AUTOSCALE_CLUSTER_ID);
        verify(clusterService).save(cluster);
        verify(executorService).submit(runnableMock);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());
        return cluster;
    }
}
