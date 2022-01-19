package com.sequenceiq.periscope.monitor.evaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
public class CronTimeEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private ScalingPolicyTargetCalculator scalingPolicyTargetCalculator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private YarnResponseUtils yarnResponseUtils;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private YarnMetricsClient yarnMetricsClient;

    @Mock
    private DateService dateService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CronTimeEvaluator underTest;

    private String fqdnBase = "testFqdn";

    private String testHostGroup = "compute";

    private String clusterCrn = "testCrn";

    @BeforeEach
    private void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        when(clusterService.findById(anyLong())).thenThrow(new RuntimeException("exception from the test"));
        underTest.run();
        verify(executorServiceWithRegistry).finished(underTest, CLUSTER_ID);
    }

    public static Stream<Arguments> scheduleBasedUpScaling() {
        return Stream.of(
                //TestCase, CurrentHostGroupCount,DesiredNodeCount, ExpectedNodeCount
                Arguments.of("SCALE_UP", 2, 7, 7),
                Arguments.of("SCALE_UP", 10, 50, 50),
                Arguments.of("SCALE_UP", 1, 15, 15),
                Arguments.of("SCALE_UP", 0, 5, 5),
                Arguments.of("SCALE_UP", 15, 15, 15)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, desiredNodeCount ={2}, expectedNodeCount={3} ")
    @MethodSource("scheduleBasedUpScaling")
    public void testPublishIfNeededForUpscaling(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScaling("SCALE_UP_MODE", currentHostGroupCount, desiredNodeCount, Optional.empty());

        assertEquals("Scheduled-Based Autoscaling Expeced Node Count should match.",
                expectedScalingCount.intValue(),
                scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue());
    }

    public static Stream<Arguments> scheduleBasedDownScaling() {
        return Stream.of(
                //TestCase, CurrentHostGroupCount,DesiredNodeCount,YarnGivenDecommissionCount,ExpectedNodeCount
                Arguments.of("SCALE_DOWN_YARN_NODE_COUNT_MATCH  ", 10, 6, 4, 6),
                Arguments.of("SCALE_DOWN_YARN_NODE_COUNT_EXTRA  ", 12, 6, 12, 6),

                Arguments.of("SCALE_DOWN_YARN_NODE_COUNT_MATCH  ", 25, 22, 3, 22),
                Arguments.of("SCALE_DOWN_YARN_NODE_COUNT_EXTRA  ", 25, 20, 25, 20)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, desiredNodeCount ={2}, yarnGivenDecommissionCount={3}, expectedNodeCount={4} ")
    @MethodSource("scheduleBasedDownScaling")
    public void testPublishIfNeededForDownScaling(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer yarnGivenDecommissionCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScaling("SCALE_DOWN_MODE", currentHostGroupCount,
                desiredNodeCount, Optional.of(yarnGivenDecommissionCount));

        int targetNodeCount = currentHostGroupCount - desiredNodeCount;
        assertEquals("Scheduled-Based Autoscaling Expeced Node Count should match.", expectedScalingCount.intValue(),
                scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue());
        assertEquals("Decommission Node Count Based on Yarn Response should match targetNodeCount.", targetNodeCount,
                scalingEvent.getDecommissionNodeIds().size());
        scalingEvent.getDecommissionNodeIds().forEach(
                nodeId -> assertTrue("Node Id hostGroup should match", nodeId.contains(testHostGroup))
        );
    }

    private ScalingEvent validateScheduleBasedScaling(String testMode, Integer currentHostGroupCount,
            Integer desiredNodeCount, Optional<Integer> yarnGivenDecommissionCount) throws Exception {

        TimeAlert alert = getAAlert(desiredNodeCount);
        StackV4Response stackV4Response = MockStackResponseGenerator
                .getMockStackV4Response(clusterCrn, testHostGroup, "testFqdn" + testHostGroup, currentHostGroupCount, false);

        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4Response);
        when(stackResponseUtils.getNodeCountForHostGroup(stackV4Response, testHostGroup)).thenCallRealMethod();
        when(scalingPolicyTargetCalculator.getDesiredAbsoluteNodeCount(any(ScalingEvent.class), anyInt())).thenCallRealMethod();
        when(dateService.isTrigger(any(TimeAlert.class), anyLong())).thenReturn(true);

        if (!"SCALE_UP_MODE".equals(testMode)) {
            YarnScalingServiceV1Response yarnScalingServiceV1Response = getMockYarnScalingResponse("test", yarnGivenDecommissionCount.get());
            when(stackResponseUtils.getCloudInstanceIdsForHostGroup(any(), any())).thenCallRealMethod();
            when(yarnMetricsClient.getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString(), any(Optional.class)))
                    .thenReturn(yarnScalingServiceV1Response);
            when(yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(anyString(), any(YarnScalingServiceV1Response.class),
                    any(Map.class), anyInt(), any(Optional.class), anyInt())).thenCallRealMethod();
        }

        underTest.publishIfNeeded(List.of(alert));

        VerificationMode verificationMode = "SCALE_UP_MODE".equals(testMode) ? never() : times(1);
        ArgumentCaptor<ScalingEvent> captor = ArgumentCaptor.forClass(ScalingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        verify(stackResponseUtils, verificationMode).getCloudInstanceIdsForHostGroup(any(), any());
        verify(yarnMetricsClient, verificationMode).getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString(), any(Optional.class));
        verify(yarnResponseUtils, verificationMode).getYarnRecommendedDecommissionHostsForHostGroup(anyString(), any(YarnScalingServiceV1Response.class),
                any(Map.class), anyInt(), any(Optional.class), anyInt());

        return captor.getValue();
    }

    private YarnScalingServiceV1Response getMockYarnScalingResponse(String instanceType, int yarnGivenDecommissionCount) {
        YarnScalingServiceV1Response yarnScalingReponse = new YarnScalingServiceV1Response();
        List decommissionCandidates = new ArrayList();
        for (int i = 1; i <= yarnGivenDecommissionCount; i++) {
            YarnScalingServiceV1Response.DecommissionCandidate decommissionCandidate = new YarnScalingServiceV1Response.DecommissionCandidate();
            decommissionCandidate.setAmCount(2);
            decommissionCandidate.setNodeId(fqdnBase + testHostGroup + i + ":8042");
            decommissionCandidates.add(decommissionCandidate);
        }
        yarnScalingReponse.setDecommissionCandidates(Map.of("candidates", decommissionCandidates));
        return yarnScalingReponse;
    }

    private TimeAlert getAAlert(int desiredNodeCount) {
        TimeAlert alert = new TimeAlert();

        Cluster cluster = new Cluster();
        cluster.setStackCrn(clusterCrn);
        alert.setCluster(cluster);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup(testHostGroup);
        scalingPolicy.setAdjustmentType(AdjustmentType.EXACT);
        scalingPolicy.setScalingAdjustment(desiredNodeCount);
        alert.setScalingPolicy(scalingPolicy);

        return alert;
    }
}
