package com.sequenceiq.periscope.monitor.evaluator.load;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response.DecommissionCandidate;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response.NewNodeManagerCandidates;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
public class YarnLoadEvaluatorTest {

    private static final long AUTOSCALE_CLUSTER_ID = 101L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    private static final int TEST_HOSTGROUP_MIN_SIZE = 3;

    private static final int TEST_HOSTGROUP_MAX_SIZE = 200;

    @InjectMocks
    private YarnLoadEvaluator underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private YarnMetricsClient yarnMetricsClient;

    private String fqdnBase = "test_fqdn";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));
        when(clusterService.findById(anyLong())).thenThrow(new RuntimeException("exception from the test"));

        underTest.run();
        verify(executorServiceWithRegistry).finished(underTest, AUTOSCALE_CLUSTER_ID);
        verify(eventPublisher).publishEvent(any(UpdateFailedEvent.class));
    }

    @Test
    public void testExecuteBeforeCoolDownPeriod() {
        Cluster cluster = getARunningCluster();
        cluster.setLastScalingActivity(Instant.now()
                .minus(2, ChronoUnit.MINUTES).toEpochMilli());
        when(clusterService.findById(anyLong())).thenReturn(cluster);
        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));
        underTest.execute();
        verify(eventPublisher, never()).publishEvent(any());
    }

    public static Stream<Arguments> dataUpScaling() {
        return Stream.of(
                            //TestCase,CurrentHostGroupCount,YarnRecommendedUpScaleCount,ExpectedUpScaleCount
                Arguments.of("SCALE_UP_ALLOWED",  3,  5, 5),
                Arguments.of("SCALE_UP_ALLOWED_AT_LIMIT", TEST_HOSTGROUP_MAX_SIZE - 10, 10, 10),
                Arguments.of("SCALE_UP_BEYOND_MAX_LIMIT", TEST_HOSTGROUP_MAX_SIZE - 10, 20, 10),
                Arguments.of("SCALE_UP_BEYOND_STEP_LIMIT", 10, 500, DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("SCALE_UP_NOT_ALLOWED", TEST_HOSTGROUP_MAX_SIZE + 2, 10, 0),
                Arguments.of("SCALE_UP_FORCED", 0, 0, 3),
                Arguments.of("SCALE_UP_FORCED", 1, 0, 2),
                Arguments.of("SCALE_UP_WHEN_HOST_SIZE_BELOW_MIN", 1, 10, 10)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, yarnRecommendedUpScaleCount ={2}, expectedUpScaleCount={3} ")
    @MethodSource("dataUpScaling")
    public void testLoadBasedUpScaling(String testType, int currentHostGroupCount,
            int yarnRecommendedUpScaleCount, int expectedUpScaleCount)  throws Exception {
        testUpScaleBasedOnYarnResponse(currentHostGroupCount, yarnRecommendedUpScaleCount, expectedUpScaleCount);
    }

    public static Stream<Arguments> dataDownScaling() {
        return Stream.of(
                            //TestCase,CurrentHostGroupCount,YarnRecommendedDecommissionCount,ExpectedDecommissionCount
                Arguments.of("DOWN_SCALE_ALLOWED",  7,  5, 7 - TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("DOWN_SCALE_FORCED",  TEST_HOSTGROUP_MAX_SIZE + 20,  0, -20),
                Arguments.of("DOWN_SCALE_BEYOND_MAX_LIMIT",  TEST_HOSTGROUP_MAX_SIZE + 20,  5, 5),
                Arguments.of("DOWN_SCALE_AT_YARN_LIMIT",  TEST_HOSTGROUP_MAX_SIZE + 20,  10, 10),
                Arguments.of("DOWN_SCALE_ALLOWED_MIN_LIMIT", 10, 10, 10 - TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("DOWN_SCALE_ALLOWED_AT_MIN_LIMIT", 20, 20 - TEST_HOSTGROUP_MIN_SIZE, 20 - TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("DOWN_SCALE_BEYOND_MIN_LIMIT", 10,  52, 10 - TEST_HOSTGROUP_MIN_SIZE),
                Arguments.of("DOWN_SCALE_NOT_ALLOWED", 0, 5, 0)
        );
    }

    @ParameterizedTest(name = "{0}: With currentHostGroupCount={1}, yarnRecommendedDecommissionCount ={2}, expectedDecommissionCount={3} ")
    @MethodSource("dataDownScaling")
    public void testLoadBasedDownScaling(String testType, int currentHostGroupCount,
            int yarnRecommendedDecommissionCount, int expectedDecommissionCount)  throws Exception {
        boolean forcedDownScale = testType.contains("FORCED") ? true : false;
        testDownScaleBasedOnYarnResponse(currentHostGroupCount, yarnRecommendedDecommissionCount, expectedDecommissionCount, forcedDownScale);
    }

    private void testDownScaleBasedOnYarnResponse(int currentHostGroupCount, int yarnDecommissionCount,
            int expectedDownScaleCount, boolean forcedDownScale) throws Exception {
        boolean scalingEventExpected = expectedDownScaleCount != 0 ? true : false;
        Optional<ScalingEvent> scalingEvent = captureScalingEvent(currentHostGroupCount, 0, yarnDecommissionCount, scalingEventExpected);

        if (scalingEventExpected) {
            int actualDownScaleCount = forcedDownScale ? scalingEvent.get().getScalingNodeCount().get() :
                    scalingEvent.get().getDecommissionNodeIds().size();
            assertEquals("ScaleDown Node Count should match.", expectedDownScaleCount, actualDownScaleCount);
        }
    }

    private void testUpScaleBasedOnYarnResponse(int currentHostGroupCount, int yarnUpScaleCount, int expectedUpscaleCount) throws Exception {
        boolean scalingEventExpected = expectedUpscaleCount != 0 ? true : false;

        Optional<ScalingEvent> scalingEvent = captureScalingEvent(currentHostGroupCount, yarnUpScaleCount, 0, scalingEventExpected);
        if (scalingEventExpected) {
            assertEquals("ScaleUp Node Count should match.", expectedUpscaleCount,
                    scalingEvent.get().getScalingNodeCount().get().intValue());
        }
    }

    private Optional<ScalingEvent> captureScalingEvent(int currentHostGroupCount, int yarnUpScaleCount,
            int yarnDownScaleCount, boolean scalingEventExpected) throws Exception {
        MockitoAnnotations.initMocks(this);
        Cluster cluster = getARunningCluster();
        String hostGroup = "compute";
        StackV4Response stackV4Response = getMockStackV4Response(hostGroup, currentHostGroupCount);

        YarnScalingServiceV1Response upScale = getMockYarnScalingResponse(hostGroup, yarnUpScaleCount, yarnDownScaleCount);

        when(clusterService.findById(anyLong())).thenReturn(cluster);
        when(cloudbreakCommunicator.getByCrn(anyString())).thenReturn(stackV4Response);
        when(stackResponseUtils.getCloudInstanceIdsForHostGroup(any(), any())).thenCallRealMethod();
        when(yarnMetricsClient.getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString()))
                .thenReturn(upScale);

        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));
        underTest.execute();

        Optional scalingEventCaptured = Optional.empty();
        if (scalingEventExpected) {
            ArgumentCaptor<ScalingEvent> captor = ArgumentCaptor.forClass(ScalingEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            scalingEventCaptured = Optional.of(captor.getValue());
        }
        return scalingEventCaptured;
    }

    private YarnScalingServiceV1Response getMockYarnScalingResponse(String instanceType, int upScaleCount, int downScaleCount) {
        NewNodeManagerCandidates.Candidate candidate = new NewNodeManagerCandidates.Candidate();
        candidate.setCount(upScaleCount);
        candidate.setModelName(instanceType);
        NewNodeManagerCandidates candidates = new NewNodeManagerCandidates();
        candidates.setCandidates(List.of(candidate));

        YarnScalingServiceV1Response yarnScalingReponse = new YarnScalingServiceV1Response();
        if (upScaleCount > 0) {
            yarnScalingReponse.setNewNMCandidates(Map.of("newNMCandidates", candidates));
        }

        List decommissionCandidates = new ArrayList();
        for (int i = 1; i <= downScaleCount; i++) {
            DecommissionCandidate decommissionCandidate = new DecommissionCandidate();
            decommissionCandidate.setAmCount(2);
            decommissionCandidate.setNodeId(fqdnBase + i + ":8042");
            decommissionCandidates.add(decommissionCandidate);
        }
        yarnScalingReponse.setDecommissionCandidates(Map.of("candidates", decommissionCandidates));
        return yarnScalingReponse;
    }

    private StackV4Response getMockStackV4Response(String hostGroup, int currentHostGroupCount) {
        Map hostGroupInstanceType = new HashMap();
        hostGroupInstanceType.put(hostGroup, "m5.xlarge");
        hostGroupInstanceType.put("master1", "m5.xlarge");
        hostGroupInstanceType.put("worker1", "m5.xlarge");

        Set fqdnToInstanceIds = new HashSet();
        for (int i = 1; i <= currentHostGroupCount; i++) {
            InstanceMetaDataV4Response metadata1 = new InstanceMetaDataV4Response();
            metadata1.setDiscoveryFQDN(fqdnBase + i);
            metadata1.setInstanceId("test_instanceid" + i);
            fqdnToInstanceIds.add(metadata1);
        }

        return MockStackResponseGenerator.getMockStackV4Response("test-crn",
                hostGroupInstanceType, fqdnToInstanceIds);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicy.setHostGroup("compute");

        LoadAlertConfiguration alertConfiguration = new LoadAlertConfiguration();
        alertConfiguration.setCoolDownMinutes(10);
        alertConfiguration.setMaxResourceValue(TEST_HOSTGROUP_MAX_SIZE);
        alertConfiguration.setMinResourceValue(TEST_HOSTGROUP_MIN_SIZE);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setScalingPolicy(scalingPolicy);
        loadAlert.setLoadAlertConfiguration(alertConfiguration);

        cluster.setLoadAlerts(Set.of(loadAlert));
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());
        return cluster;
    }
}
