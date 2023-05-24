package com.sequenceiq.periscope.monitor.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.controller.validation.AlertValidator;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
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
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
class CronTimeEvaluatorTest {

    private static final long CLUSTER_ID = 1L;

    private static final Long TEST_ACTIVITY_ID = 2L;

    private static final String TEST_FQDN_BASE = "testFqdn";

    private static final String TEST_HOSTGROUP_COMPUTE = "compute";

    private static final String TEST_HOSTGROUP_EXECUTOR = "executor";

    private static final String TEST_CLUSTERCRN = "testCrn";

    private static final String TEST_MACHINE_USER_CRN = "testMachineUserCrn";

    private static final String TEST_USER_CRN = "testUserCrn";

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
    private ScalingActivityService scalingActivityService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private DateService dateService;

    @Mock
    private Clock clock;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CronTimeEvaluator underTest;

    @Mock
    private AlertValidator alertValidator;

    @Test
    void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(CLUSTER_ID));
        when(clusterService.findById(anyLong())).thenThrow(new RuntimeException("exception from the test"));

        try {
            underTest.run();
            fail("expected runtimeException");
        } catch (RuntimeException e) {
            assertEquals("exception from the test", e.getMessage());
        }

        verify(executorServiceWithRegistry).finished(underTest, CLUSTER_ID);
    }

    static Stream<Arguments> scheduleBasedUpScaling() {
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
    void testPublishIfNeededForUpscaling(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScaling("SCALE_UP_MODE", currentHostGroupCount, desiredNodeCount, Optional.empty());

        assertEquals(expectedScalingCount.intValue(), scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue(),
                "Scheduled-Based Autoscaling Expeced Node Count should match.");
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, desiredNodeCount ={2}, expectedNodeCount={3} ")
    @MethodSource("scheduleBasedUpScaling")
    void testPublishIfNeededForUpscalingWithImpalaOnly(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScalingWithImpalaOnly("SCALE_UP_MODE", currentHostGroupCount, desiredNodeCount, Optional.empty());

        assertEquals(expectedScalingCount.intValue(), scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue(),
                "Scheduled-Based Autoscaling Expeced Node Count should match.");
    }

    static Stream<Arguments> scheduleBasedDownScaling() {
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
    void testPublishIfNeededForDownScaling(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer yarnGivenDecommissionCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScaling("SCALE_DOWN_MODE", currentHostGroupCount,
                desiredNodeCount, Optional.of(yarnGivenDecommissionCount));

        int targetNodeCount = currentHostGroupCount - desiredNodeCount;
        assertEquals(expectedScalingCount.intValue(), scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue(),
                "Scheduled-Based Autoscaling Expeced Node Count should match.");
        assertEquals(targetNodeCount, scalingEvent.getDecommissionNodeIds().size(),
                "Decommission Node Count Based on Yarn Response should match targetNodeCount.");
        scalingEvent.getDecommissionNodeIds().forEach(
                nodeId -> assertTrue(nodeId.contains(TEST_HOSTGROUP_COMPUTE), "Node Id hostGroup should match")
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, desiredNodeCount ={2}, yarnGivenDecommissionCount={3}, expectedNodeCount={4} ")
    @MethodSource("scheduleBasedDownScaling")
    void testPublishIfNeededForDownScalingWithImpalaOnly(String testType, Integer currentHostGroupCount,
            Integer desiredNodeCount, Integer yarnGivenDecommissionCount, Integer expectedScalingCount) throws Exception {

        ScalingEvent scalingEvent = validateScheduleBasedScalingWithImpalaOnly("SCALE_DOWN_MODE", currentHostGroupCount,
                desiredNodeCount, Optional.of(yarnGivenDecommissionCount));

        int targetNodeCount = currentHostGroupCount - desiredNodeCount;
        assertEquals(expectedScalingCount.intValue(), scalingEvent.getDesiredAbsoluteHostGroupNodeCount().intValue(),
                "Scheduled-Based Autoscaling Expeced Node Count should match.");
        assertEquals(0, scalingEvent.getDecommissionNodeIds().size(),
                "Yarn Service Not Present. So recommodations from Yarn:.");
    }

    private ScalingEvent validateScheduleBasedScaling(String testMode, Integer currentHostGroupCount,
            Integer desiredNodeCount, Optional<Integer> yarnGivenDecommissionCount) throws Exception {

        Cluster cluster = getACluster();
        TimeAlert alert = getAAlert(cluster, desiredNodeCount);
        ScalingActivity activity = getActivity(cluster);
        StackV4Response stackV4Response = MockStackResponseGenerator
                .getMockStackV4Response(TEST_CLUSTERCRN, TEST_HOSTGROUP_COMPUTE, "testFqdn" + TEST_HOSTGROUP_COMPUTE, currentHostGroupCount, 0);

        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4Response);
        when(scalingActivityService.create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong())).thenReturn(activity);
        when(messagesService.getMessageWithArgs(anyString(), any())).thenReturn("test-message");
        when(stackResponseUtils.getNodeCountForHostGroup(stackV4Response, TEST_HOSTGROUP_COMPUTE)).thenCallRealMethod();
        when(scalingPolicyTargetCalculator.getDesiredAbsoluteNodeCount(any(ScalingEvent.class), anyInt())).thenCallRealMethod();
        when(dateService.isTrigger(any(TimeAlert.class), anyLong())).thenReturn(true);
        when(stackResponseUtils.getServicesOnHostGroup(stackV4Response, TEST_HOSTGROUP_COMPUTE)).thenReturn(Set.of("impala", "yarn"));

        if (!"SCALE_UP_MODE".equals(testMode)) {
            YarnScalingServiceV1Response yarnScalingServiceV1Response = getMockYarnScalingResponse("test", yarnGivenDecommissionCount.get());
            when(stackResponseUtils.getCloudInstanceIdsForHostGroup(any(), any())).thenCallRealMethod();
            when(yarnMetricsClient.getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString(), any(), any(Optional.class)))
                    .thenReturn(yarnScalingServiceV1Response);
            when(yarnResponseUtils.getYarnRecommendedDecommissionHostsForHostGroup(any(YarnScalingServiceV1Response.class),
                    anyMap())).thenCallRealMethod();
        }

        underTest.publishIfNeeded(List.of(alert));

        VerificationMode verificationMode = "SCALE_UP_MODE".equals(testMode) ? never() : times(1);
        ArgumentCaptor<ScalingEvent> captor = ArgumentCaptor.forClass(ScalingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        verify(stackResponseUtils, verificationMode).getCloudInstanceIdsForHostGroup(any(), any());
        verify(yarnMetricsClient, verificationMode).getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString(), anyString(),
                any(Optional.class));
        verify(yarnResponseUtils, verificationMode).getYarnRecommendedDecommissionHostsForHostGroup(any(YarnScalingServiceV1Response.class),
                anyMap());

        return captor.getValue();
    }

    private ScalingEvent validateScheduleBasedScalingWithImpalaOnly(String testMode, Integer currentHostGroupCount,
            Integer desiredNodeCount, Optional<Integer> yarnGivenDecommissionCount) throws Exception {

        Cluster cluster = getACluster();
        TimeAlert alert = getAAlert(cluster, desiredNodeCount);
        ScalingActivity activity = getActivity(cluster);
        StackV4Response stackV4Response = MockStackResponseGenerator
                .getMockStackV4Response(TEST_CLUSTERCRN, TEST_HOSTGROUP_COMPUTE,
                        "testFqdn" + TEST_HOSTGROUP_COMPUTE, currentHostGroupCount, 0);

        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4Response);
        when(scalingActivityService.create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong())).thenReturn(activity);
        when(messagesService.getMessageWithArgs(anyString(), any())).thenReturn("test-message");
        when(stackResponseUtils.getServicesOnHostGroup(stackV4Response, TEST_HOSTGROUP_COMPUTE)).thenReturn(Set.of("impala"));
        when(stackResponseUtils.getNodeCountForHostGroup(stackV4Response, TEST_HOSTGROUP_COMPUTE)).thenCallRealMethod();
        when(scalingPolicyTargetCalculator.getDesiredAbsoluteNodeCount(any(ScalingEvent.class), anyInt())).thenCallRealMethod();
        when(dateService.isTrigger(any(TimeAlert.class), anyLong())).thenReturn(true);

        underTest.publishIfNeeded(List.of(alert));

        ArgumentCaptor<ScalingEvent> captor = ArgumentCaptor.forClass(ScalingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        return captor.getValue();
    }

    private YarnScalingServiceV1Response getMockYarnScalingResponse(String instanceType, int yarnGivenDecommissionCount) {
        YarnScalingServiceV1Response yarnScalingReponse = new YarnScalingServiceV1Response();
        List decommissionCandidates = new ArrayList();
        for (int i = 1; i <= yarnGivenDecommissionCount; i++) {
            YarnScalingServiceV1Response.DecommissionCandidate decommissionCandidate = new YarnScalingServiceV1Response.DecommissionCandidate();
            decommissionCandidate.setAmCount(2);
            decommissionCandidate.setNodeId(TEST_FQDN_BASE + TEST_HOSTGROUP_COMPUTE + i + ":8042");
            decommissionCandidates.add(decommissionCandidate);
        }
        yarnScalingReponse.setDecommissionCandidates(Map.of("candidates", decommissionCandidates));
        return yarnScalingReponse;
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTERCRN);
        cluster.setMachineUserCrn(TEST_MACHINE_USER_CRN);
        return cluster;
    }

    private ScalingActivity getActivity(Cluster cluster) {
        ScalingActivity activity = new ScalingActivity();
        activity.setId(TEST_ACTIVITY_ID);
        activity.setCluster(cluster);
        return activity;
    }

    private TimeAlert getAAlert(Cluster cluster, int desiredNodeCount) {
        TimeAlert alert = new TimeAlert();

        alert.setCluster(cluster);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setUserCrn(TEST_USER_CRN);
        cluster.setClusterPertain(clusterPertain);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup(TEST_HOSTGROUP_COMPUTE);
        scalingPolicy.setAdjustmentType(AdjustmentType.EXACT);
        scalingPolicy.setScalingAdjustment(desiredNodeCount);
        alert.setScalingPolicy(scalingPolicy);

        return alert;
    }
}
