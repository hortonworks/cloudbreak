package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_DOWN_STEP_SIZE;
import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.DEFAULT_MAX_SCALE_UP_STEP_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.sender.ScalingEventSender;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
class YarnBasedScalingAdjustmentServiceTest {

    private static final Long CLUSTER_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-46d7-b645-7ccf9edbb73d:cluster:1ce07c57-74be-4egd-820d-e81a98d9e151";

    private static final String TEST_MESSAGE = "test message";

    private static final int HOSTGROUP_MIN_SIZE = 3;

    private static final int HOSTGROUP_MAX_SIZE = 200;

    private static final Integer COOLDOWN_MINUTES = 2;

    private static final String FQDN_BASE = "fqdn-";

    private static final String POLLING_USER_CRN = "pollingUserCrn";

    private static final String HOSTGROUP = "compute";

    @Mock
    private YarnMetricsClient yarnMetricsClient;

    @Mock
    private YarnResponseUtils yarnResponseUtils;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private PeriscopeMetricService metricService;

    @Mock
    private Clock clock;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private ScalingEventSender scalingEventSender;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ScalingActivity scalingActivity;

    @Captor
    private ArgumentCaptor<ScalingEvent> eventCaptor;

    @InjectMocks
    private YarnBasedScalingAdjustmentService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scalingEventSender, "eventPublisher", eventPublisher);
    }

    public static Stream<Arguments> dataUpScaling() {
        return Stream.of(
                //TestCase,CurrentHostGroupCount,yarnRecommendedScaleUpCount,DesiredAbsoluteHostGroupNodeCount
                Arguments.of("SCALE_UP_ALLOWED_1", 3, 5, 8),
                Arguments.of("SCALE_UP_ALLOWED_2", 6, 35, 41),
                Arguments.of("SCALE_UP_ALLOWED_AT_LIMIT", HOSTGROUP_MAX_SIZE - 10, 10, HOSTGROUP_MAX_SIZE),
                Arguments.of("SCALE_UP_BEYOND_MAX_LIMIT", HOSTGROUP_MAX_SIZE - 10, 20, HOSTGROUP_MAX_SIZE),
                Arguments.of("SCALE_UP_BEYOND_STEP_LIMIT_1", 10, 500, 10 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("SCALE_UP_BEYOND_STEP_LIMIT_2", 13, 150, 13 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("SCALE_UP_NOT_ALLOWED_1", HOSTGROUP_MAX_SIZE + 2, 10, 0),
                Arguments.of("SCALE_UP_NOT_ALLOWED_2", HOSTGROUP_MAX_SIZE, 10, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataUpScaling")
    void testLoadBasedUpscaling(String testCase, int currentHostGroupCount, int yarnRecommendedScaleUpCount, int desiredAbsoluteHostGroupNodeCount)
            throws Exception {
        Cluster cluster = getCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(STACK_CRN, HOSTGROUP, FQDN_BASE, currentHostGroupCount, 0);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, yarnRecommendedScaleUpCount, 0);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (desiredAbsoluteHostGroupNodeCount - Math.min(yarnRecommendedScaleUpCount, DEFAULT_MAX_SCALE_UP_STEP_SIZE) > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(currentHostGroupCount);
            // Including 1 master and 2 worker nodes from MockStackResponseGenerator
            assertThat(result.getExistingClusterNodeCount()).isEqualTo(currentHostGroupCount + 3);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(desiredAbsoluteHostGroupNodeCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(currentHostGroupCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }
    }

    public static Stream<Arguments> dataUpScalingWithUnhealthyInstances() {
        return Stream.of(
                // TestCase,healthyHostGroupNodeCount,unhealthyHostGroupNodeCount,yarnRecommendedUpScaleUpCount,desiredAbsoluteHostGroupNodeCount
                Arguments.of("SCALE_UP_ALLOWED_1", 3, 2, 2, 7),
                Arguments.of("SCALE_UP_ALLOWED_2", 38, 35, 77, 150),
                Arguments.of("SCALE_UP_ALLOWED_3", 1, 2, 3, 6),
                Arguments.of("SCALE_UP_ALLOWED_BEYOND_STEP_LIMIT_1", 35, 47, 123, 35 + 47 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("SCALE_UP_ALLOWED_BEYOND_STEP_LIMIT_2", 40, 3, 257, 40 + 3 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("SCALE_UP_ALLOWED_AT_LIMIT_1", HOSTGROUP_MAX_SIZE - 15, 10, 5, HOSTGROUP_MAX_SIZE),
                Arguments.of("SCALE_UP_ALLOWED_AT_LIMIT_2", HOSTGROUP_MAX_SIZE - 25, 10, 15, HOSTGROUP_MAX_SIZE),
                Arguments.of("SCALE_UP_BEYOND_MAX_LIMIT", HOSTGROUP_MAX_SIZE - 20, 15, 10, HOSTGROUP_MAX_SIZE),
                Arguments.of("SCALE_UP_WHEN_SIZE_BELOW_MIN", 2, 1, 3, 6),
                Arguments.of("SCALE_UP_NOT_ALLOWED_1", HOSTGROUP_MAX_SIZE - 10, 10, 13, 0),
                Arguments.of("SCALE_UP_NOT_ALLOWED_2", HOSTGROUP_MAX_SIZE - 10, 10, 10, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataUpScalingWithUnhealthyInstances")
    void testLoadBasedUpscalingWithUnhealthyInstances(String testCase, int healthyHostGroupNodeCount, int unhealthyHostGroupNodeCount,
            int yarnRecommendedScaleUpCount, int desiredAbsoluteHostGroupNodeCount) throws Exception {
        Cluster cluster = getCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(STACK_CRN, HOSTGROUP, FQDN_BASE,
                healthyHostGroupNodeCount + unhealthyHostGroupNodeCount, unhealthyHostGroupNodeCount);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, yarnRecommendedScaleUpCount, 0);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (desiredAbsoluteHostGroupNodeCount - Math.min(yarnRecommendedScaleUpCount, DEFAULT_MAX_SCALE_UP_STEP_SIZE) > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(healthyHostGroupNodeCount + unhealthyHostGroupNodeCount);
            assertThat(result.getExistingClusterNodeCount()).isEqualTo(healthyHostGroupNodeCount + unhealthyHostGroupNodeCount + 3);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(desiredAbsoluteHostGroupNodeCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(healthyHostGroupNodeCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }

    }

    public static Stream<Arguments> dataUpScalingForStopStart() {
        return Stream.of(
                //TestCase,runningHostGroupNodeCount,stoppedHostGroupNodeCount,yarnRecommendedUpScaleUpCount,desiredAbsoluteHostGroupNodeCount
                Arguments.of("STOP_START_SCALE_UP_ALLOWED_1", 3, HOSTGROUP_MAX_SIZE - 3, 2, 5),
                Arguments.of("STOP_START_SCALE_UP_ALLOWED_2", 17, HOSTGROUP_MAX_SIZE - 17, 77, 94),
                Arguments.of("STOP_START_SCALE_UP_ALLOWED_AT_LIMIT_1", HOSTGROUP_MAX_SIZE - 10, 10, 10, HOSTGROUP_MAX_SIZE),
                Arguments.of("STOP_START_SCALE_UP_ALLOWED_AT_LIMIT_2", HOSTGROUP_MAX_SIZE - 43, 43, 43, HOSTGROUP_MAX_SIZE),
                Arguments.of("STOP_START_SCALE_UP_BEYOND_STEP_LIMIT_1", 24, HOSTGROUP_MAX_SIZE - 24, 146, 24 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("STOP_START_SCALE_UP_BEYOND_STEP_LIMIT_2", 3, HOSTGROUP_MAX_SIZE - 3, 157, 3 + DEFAULT_MAX_SCALE_UP_STEP_SIZE),
                Arguments.of("STOP_START_SCALE_UP_BEYOND_MAX_LIMIT", HOSTGROUP_MAX_SIZE - 10, 10, 20, HOSTGROUP_MAX_SIZE),
                Arguments.of("STOP_START_SCALE_UP_NOT_ALLOWED_1", HOSTGROUP_MAX_SIZE, 0, 43, 0),
                Arguments.of("STOP_START_SCALE_UP_NOT_ALLOWED_2", HOSTGROUP_MAX_SIZE, 0, 64, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataUpScalingForStopStart")
    void testLoadBasedUpscalingWithStopStart(String testCase, int runningHostGroupNodeCount, int stoppedHostGroupNodeCount, int yarnRecommendedScaleUpCount,
            int desiredAbsoluteHostGroupNodeCount) throws Exception {
        Cluster cluster = getCluster();
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(STACK_CRN, HOSTGROUP, FQDN_BASE,
                runningHostGroupNodeCount, stoppedHostGroupNodeCount);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, yarnRecommendedScaleUpCount, 0);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (desiredAbsoluteHostGroupNodeCount - Math.min(yarnRecommendedScaleUpCount, DEFAULT_MAX_SCALE_UP_STEP_SIZE) > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(STOPSTART);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(runningHostGroupNodeCount);
            assertThat(result.getExistingClusterNodeCount()).isEqualTo(runningHostGroupNodeCount + 3);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(desiredAbsoluteHostGroupNodeCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(runningHostGroupNodeCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }
    }

    public static Stream<Arguments> dataDownScaling() {
        return Stream.of(
                //TestCase,CurrentHostGroupCount,YarnRecommendedDecommissionCount,ExpectedDecommissionCount
                Arguments.of("SCALE_DOWN_ALLOWED_1", 7, 5, 7 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_2", 18, 7, 7),
                Arguments.of("SCALE_DOWN_RECOMMENDED", HOSTGROUP_MAX_SIZE, 100, 100),
                Arguments.of("SCALE_DOWN_BEYOND_STEP_LIMIT", 200, 150, 100),
                Arguments.of("SCALE_DOWN_ALLOWED_MIN_LIMIT", 10, 10, 10 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_AT_MIN_LIMIT", 20, 20 - HOSTGROUP_MIN_SIZE, 20 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_BEYOND_MIN_LIMIT", 10, 52, 10 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_NOT_ALLOWED", 0, 5, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataDownScaling")
    void testLoadBasedDownscaling(String testCase, int currentHostGroupNodeCount, int yarnRecommendedDecommissionCount, int expectedDecommissionCount)
            throws Exception {
        Cluster cluster = getCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(STACK_CRN, HOSTGROUP, FQDN_BASE, currentHostGroupNodeCount, 0);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, 0, yarnRecommendedDecommissionCount);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (expectedDecommissionCount > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount - expectedDecommissionCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
            assertThat(result.getDecommissionNodeIds()).hasSize(expectedDecommissionCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }
    }

    public static Stream<Arguments> dataDownScalingWithUnhealthyInstances() {
        return Stream.of(
                //TestCase,HealthyHostGroupCount,UnhealthyHostGroupCount,YarnRecommendedDecommissionCount,ExpectedDecommissionCount
                Arguments.of("SCALE_DOWN_ALLOWED_1", 15, 6, 18, 15 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_2", 34, 41, 47, 34 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_3", 113, 12, 78, 78),
                Arguments.of("SCALE_DOWN_ALLOWED_4", 27, 4, 16, 16),
                Arguments.of("SCALE_DOWN_BEYOND_STEP_LIMIT_1", 134, 17, 150, DEFAULT_MAX_SCALE_DOWN_STEP_SIZE),
                Arguments.of("SCALE_DOWN_BEYOND_STEP_LIMIT_2", HOSTGROUP_MAX_SIZE - 65, 65, 108, DEFAULT_MAX_SCALE_DOWN_STEP_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_BEYOND_MIN_LIMIT_1", 16, 5, 23, 16 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_ALLOWED_BEYOND_MIN_LIMIT_2", 47, 12, 83, 47 - HOSTGROUP_MIN_SIZE),
                Arguments.of("SCALE_DOWN_NOT_ALLOWED", 0, 5, 3, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataDownScalingWithUnhealthyInstances")
    void testLoadBasedDownscalingWithUnhealthyInstances(String testCase, int healthyHostGroupNodeCount, int unhealthyHostGroupNodeCount,
            int yarnRecommendedDecommissionNodeCount, int expectedDecommissionNodeCount) throws Exception {
        Cluster cluster = getCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(STACK_CRN, HOSTGROUP, FQDN_BASE,
                healthyHostGroupNodeCount + unhealthyHostGroupNodeCount, unhealthyHostGroupNodeCount);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, 0, yarnRecommendedDecommissionNodeCount);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (expectedDecommissionNodeCount > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(healthyHostGroupNodeCount);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(healthyHostGroupNodeCount - expectedDecommissionNodeCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(healthyHostGroupNodeCount);
            assertThat(result.getDecommissionNodeIds()).hasSize(expectedDecommissionNodeCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }
    }

    public static Stream<Arguments> dataDownScalingForStopStart() {
        return Stream.of(
                //Testcase,runningHostGroupNodeCount,stoppedHostGroupNodeCount,yarnRecommendedDecommissionCount,expectedDecommissionCount
                Arguments.of("STOP_START_SCALE_DOWN_ALLOWED_1", 27, HOSTGROUP_MAX_SIZE - 27, 16, 16),
                Arguments.of("STOP_START_SCALE_DOWN_ALLOWED_2", HOSTGROUP_MAX_SIZE, 0, 78, 78),
                Arguments.of("STOP_START_SCALE_DOWN_BEYOND_STEP_LIMIT_1", 117, HOSTGROUP_MAX_SIZE - 117, 103, DEFAULT_MAX_SCALE_DOWN_STEP_SIZE),
                Arguments.of("STOP_START_SCALE_DOWN_BEYOND_STEP_LIMIT_2", HOSTGROUP_MAX_SIZE, 0, 153, DEFAULT_MAX_SCALE_DOWN_STEP_SIZE),
                Arguments.of("STOP_START_SCALE_DOWN_BEYOND_LIMIT_1", 23, HOSTGROUP_MAX_SIZE - 23, 30, 23 - HOSTGROUP_MIN_SIZE),
                Arguments.of("STOP_START_SCALE_DOWN_BEYOND_LIMIT_2", 47, HOSTGROUP_MAX_SIZE - 47, 83, 47 - HOSTGROUP_MIN_SIZE),
                Arguments.of("STOP_START_SCALE_DOWN_NOT_ALLOWED_1", HOSTGROUP_MIN_SIZE, HOSTGROUP_MAX_SIZE - HOSTGROUP_MIN_SIZE, 35, 0),
                Arguments.of("STOP_START_SCALE_DOWN_NOT_ALLOWED_2", 0, HOSTGROUP_MAX_SIZE, 3, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataDownScalingForStopStart")
    void testLoadBasedDownscalingWithStopStart(String testCase, int runningHostGroupNodeCount, int stoppedHostGroupNodeCount,
            int yarnRecommendedDecommissionNodeCount, int expectedDecommissionNodeCount) throws Exception {
        Cluster cluster = getCluster();
        cluster.setStopStartScalingEnabled(Boolean.TRUE);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(STACK_CRN, HOSTGROUP, FQDN_BASE,
                runningHostGroupNodeCount, stoppedHostGroupNodeCount);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, 0, yarnRecommendedDecommissionNodeCount);

        setupBasicMocks(stackResponse, yarnResponse);

        underTest.pollYarnMetricsAndScaleCluster(cluster, POLLING_USER_CRN, Boolean.TRUE.equals(cluster.isStopStartScalingEnabled()), stackResponse);

        verify(metricService, times(1)).recordYarnInvocation(eq(cluster), anyLong());
        if (expectedDecommissionNodeCount > 0) {
            verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(STOPSTART);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(runningHostGroupNodeCount);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(runningHostGroupNodeCount - expectedDecommissionNodeCount);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(runningHostGroupNodeCount);
            assertThat(result.getDecommissionNodeIds()).hasSize(expectedDecommissionNodeCount);
        } else {
            verify(eventPublisher, never()).publishEvent(any(ScalingEvent.class));
        }
    }

    private YarnScalingServiceV1Response getMockYarnScalingResponse(String hostGroup, int upScaleCount, int downScaleCount) {
        YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate candidate = new YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate();
        candidate.setCount(upScaleCount);
        candidate.setModelName(hostGroup);
        YarnScalingServiceV1Response.NewNodeManagerCandidates candidates = new YarnScalingServiceV1Response.NewNodeManagerCandidates();
        candidates.setCandidates(List.of(candidate));

        YarnScalingServiceV1Response yarnScalingReponse = new YarnScalingServiceV1Response();
        if (upScaleCount > 0) {
            yarnScalingReponse.setNewNMCandidates(candidates);
        }

        List<YarnScalingServiceV1Response.DecommissionCandidate> decommissionCandidates = new ArrayList<>();
        IntStream.range(1, downScaleCount + 1).forEach(i -> {
            YarnScalingServiceV1Response.DecommissionCandidate decommissionCandidate = new YarnScalingServiceV1Response.DecommissionCandidate();
            decommissionCandidate.setAmCount(2);
            decommissionCandidate.setNodeId(FQDN_BASE + i + ":8042");
            decommissionCandidates.add(decommissionCandidate);
        });
        yarnScalingReponse.setDecommissionCandidates(Map.of("candidates", decommissionCandidates));

        return yarnScalingReponse;
    }

    private Cluster getCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        cluster.setStackCrn(STACK_CRN);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStopStartScalingEnabled(Boolean.FALSE);
        cluster.setState(ClusterState.RUNNING);
        cluster.setLastScalingActivity(Instant.now().minus(45, ChronoUnit.MINUTES).toEpochMilli());

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicy.setHostGroup(HOSTGROUP);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setScalingPolicy(scalingPolicy);
        loadAlert.setCluster(cluster);

        LoadAlertConfiguration loadAlertConfiguration = new LoadAlertConfiguration();
        loadAlertConfiguration.setMinResourceValue(HOSTGROUP_MIN_SIZE);
        loadAlertConfiguration.setMaxResourceValue(HOSTGROUP_MAX_SIZE);
        loadAlertConfiguration.setCoolDownMinutes(COOLDOWN_MINUTES);
        loadAlert.setLoadAlertConfiguration(loadAlertConfiguration);

        cluster.setLoadAlerts(Set.of(loadAlert));
        return cluster;
    }

    private void setupBasicMocks(StackV4Response stackResponse, YarnScalingServiceV1Response yarnResponse) throws Exception {
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsForHostGroup(any(StackV4Response.class), anyString());
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsWithServicesHealthyForHostGroup(any(StackV4Response.class), anyString());
        doCallRealMethod().when(stackResponseUtils).getStoppedCloudInstanceIdsInHostGroup(any(StackV4Response.class), anyString());
        doReturn(yarnResponse).when(yarnMetricsClient).getYarnMetricsForCluster(any(Cluster.class), eq(stackResponse), eq(HOSTGROUP), eq(POLLING_USER_CRN),
                any(Optional.class));
        doReturn(TEST_MESSAGE).when(messagesService).getMessageWithArgs(anyString(), anyInt(), anyList(), anyInt(), anyList());
        doNothing().when(metricService).recordYarnInvocation(any(Cluster.class), anyLong());
        lenient().doCallRealMethod().when(clock).getCurrentTimeMillis();
        lenient().doReturn(scalingActivity).when(scalingActivityService)
                .create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong(), anyLong(), anyString());
        lenient().doCallRealMethod().when(scalingEventSender).sendScaleUpEvent(any(BaseAlert.class), anyInt(), anyInt(), anyInt(), anyInt(), anyLong());
        lenient().doCallRealMethod().when(scalingEventSender).sendStopStartScaleUpEvent(any(BaseAlert.class), anyInt(), anyInt(), anyInt(), anyLong());
        lenient().doCallRealMethod().when(scalingEventSender).sendScaleDownEvent(any(BaseAlert.class), anyInt(), anyList(), anyInt(),
                any(ScalingAdjustmentType.class), anyLong());
        lenient().doNothing().when(eventPublisher).publishEvent(any(ScalingEvent.class));
        doCallRealMethod().when(yarnResponseUtils).getYarnRecommendedScaleUpCount(any(YarnScalingServiceV1Response.class), anyString());
        doCallRealMethod().when(yarnResponseUtils).getYarnRecommendedDecommissionHostsForHostGroup(any(YarnScalingServiceV1Response.class), anyMap());
    }
}
