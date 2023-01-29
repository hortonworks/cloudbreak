package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.yarn.YarnScalingServiceV1Response;
import com.sequenceiq.periscope.monitor.client.YarnMetricsClient;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.evaluator.load.YarnResponseUtils;
import com.sequenceiq.periscope.monitor.event.ScalingEvent;
import com.sequenceiq.periscope.monitor.sender.ScalingEventSender;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
class RegularScalingAdjustmentServiceTest {

    private static final Long CLUSTER_ID = 1L;

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:9d74eee4-1cad-46d7-b645-7ccf9edbb73d:cluster:1ce07c57-74be-4egd-820d-e81a98d9e151";

    private static final String TEST_MESSAGE = " test message";

    private static final Integer COOLDOWN_MINUTES = 2;

    private static final String HOSTGROUP = "compute";

    private static final String FQDN_BASE = "fqdn-";

    private static final String POLLING_USER_CRN = "someUserCrn";

    @Mock
    private YarnMetricsClient yarnMetricsClient;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private YarnResponseUtils yarnResponseUtils;

    @Mock
    private ScalingEventSender scalingEventSender;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private Clock clock;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Captor
    private ArgumentCaptor<ScalingEvent> eventCaptor;

    @InjectMocks
    private RegularScalingAdjustmentService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scalingEventSender, "eventPublisher", eventPublisher);
    }

    public static Stream<Arguments> dataForMandatoryRegularAdjustment() {
        return Stream.of(
                //TestCase,minResourceValue,maxResourceValue,currentHostGroupNodeCount,expectedUpscaleAdjustment,expectedDownscaleAdjustment
                Arguments.of("MANDATORY_UPSCALE_1", 4, 26, 2, 2, 0),
                Arguments.of("MANDATORY_UPSCALE_2", 15, 45, 11, 4, 0),
                Arguments.of("MANDATORY_DOWNSCALE_1", 73, 138, 150, 0, 12),
                Arguments.of("MANDATORY_DOWNSCALE_2", 45, 49, 53, 0, 4),
                Arguments.of("MANDATORY_ADJ_NOT_REQD_1", 17, 33, 26, 0, 0),
                Arguments.of("MANDATORY_ADJ_NOT_REQD_2", 20, 56, 37, 0, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("dataForMandatoryRegularAdjustment")
    void testRegularUpscaleAndDownscaleAdjustments(String testCase, int minResourceValue, int maxResourceValue, int currentHostGroupNodeCount,
            int expectedUpscaleAdjustment, int expectedDownscaleAdjustment) throws Exception {
        Cluster cluster = getCluster(minResourceValue, maxResourceValue);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(STACK_CRN, HOSTGROUP, FQDN_BASE, currentHostGroupNodeCount, 0);
        YarnScalingServiceV1Response yarnResponse = getMockYarnScalingResponse(HOSTGROUP, expectedUpscaleAdjustment, expectedDownscaleAdjustment);
        MandatoryScalingAdjustmentParameters mandatoryAdjustmentParams = mockAdjustmentParams(minResourceValue, maxResourceValue,
                currentHostGroupNodeCount, currentHostGroupNodeCount);

        setupBasicMocks(yarnResponse);

        underTest.performMandatoryAdjustment(cluster, POLLING_USER_CRN, stackResponse, mandatoryAdjustmentParams);

        if (expectedUpscaleAdjustment > 0) {
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verifyNoInteractions(yarnMetricsClient);
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount + expectedUpscaleAdjustment);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
            assertThat(result.getExistingClusterNodeCount()).isEqualTo(currentHostGroupNodeCount + 3);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
        } else if (expectedDownscaleAdjustment > 0) {
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(yarnMetricsClient, times(1)).getYarnMetricsForCluster(cluster, stackResponse, HOSTGROUP, POLLING_USER_CRN,
                    Optional.of(mandatoryAdjustmentParams.getDownscaleAdjustment()));
            ScalingEvent result = eventCaptor.getValue();
            assertThat(result.getAlert()).isInstanceOf(LoadAlert.class);
            assertThat(result.getScalingAdjustmentType()).isEqualTo(REGULAR);
            assertThat(result.getDecommissionNodeIds()).hasSize(expectedDownscaleAdjustment);
            assertThat(result.getExistingServiceHealthyHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
            assertThat(result.getDesiredAbsoluteHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount - expectedDownscaleAdjustment);
            assertThat(result.getExistingHostGroupNodeCount()).isEqualTo(currentHostGroupNodeCount);
        } else {
            verifyNoInteractions(eventPublisher, yarnMetricsClient);
        }
    }

    private MandatoryScalingAdjustmentParameters mockAdjustmentParams(int minResourceValue, int maxResourceValue,
            int serviceHealthyHostGroupNodeCount, int existingHostGroupNodeCount) {
        MandatoryScalingAdjustmentParameters adjustmentParams = mock(MandatoryScalingAdjustmentParameters.class);

        lenient()
                .doReturn(Optional.of(serviceHealthyHostGroupNodeCount - minResourceValue).filter(val -> val < 0).map(Math::abs).orElse(null))
                .when(adjustmentParams).getUpscaleAdjustment();

        lenient()
                .doReturn(Optional.of(maxResourceValue - existingHostGroupNodeCount).filter(val -> val < 0).map(Math::abs).orElse(null))
                .when(adjustmentParams).getDownscaleAdjustment();

        return adjustmentParams;
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

    private Cluster getCluster(int minResourceValue, int maxResourceValue) {
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
        loadAlertConfiguration.setMinResourceValue(minResourceValue);
        loadAlertConfiguration.setMaxResourceValue(maxResourceValue);
        loadAlertConfiguration.setCoolDownMinutes(COOLDOWN_MINUTES);
        loadAlert.setLoadAlertConfiguration(loadAlertConfiguration);

        cluster.setLoadAlerts(Set.of(loadAlert));
        return cluster;
    }

    private void setupBasicMocks(YarnScalingServiceV1Response yarnResponse) throws Exception {
        ScalingActivity scalingActivity = mock(ScalingActivity.class);

        lenient().doCallRealMethod().when(scalingEventSender).sendScaleUpEvent(any(BaseAlert.class), anyInt(), anyInt(), anyInt(), anyInt(), anyLong());
        lenient().doCallRealMethod().when(scalingEventSender).sendStopStartScaleUpEvent(any(BaseAlert.class), anyInt(), anyInt(), anyInt(), anyLong());
        lenient().doCallRealMethod().when(scalingEventSender).sendScaleDownEvent(any(BaseAlert.class), anyInt(), anyList(), anyInt(),
                any(ScalingAdjustmentType.class), anyLong());
        lenient().doReturn(TEST_MESSAGE).when(messagesService).getMessageWithArgs(anyString(), any());
        lenient().doCallRealMethod().when(clock).getCurrentTimeMillis();
        lenient().doReturn(scalingActivity).when(scalingActivityService).create(any(Cluster.class), any(ActivityStatus.class), anyString(), anyLong());
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsForHostGroup(any(StackV4Response.class), anyString());
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsWithServicesHealthyForHostGroup(any(StackV4Response.class), anyString());
        lenient().doReturn(yarnResponse).when(yarnMetricsClient).getYarnMetricsForCluster(any(Cluster.class), any(StackV4Response.class), anyString(),
                anyString(), any(Optional.class));
        lenient().doCallRealMethod().when(yarnResponseUtils).getYarnRecommendedDecommissionHostsForHostGroup(any(YarnScalingServiceV1Response.class), anyMap());
    }
}