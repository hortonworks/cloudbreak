package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.model.ScalingAdjustmentType.REGULAR;
import static com.sequenceiq.periscope.model.ScalingAdjustmentType.STOPSTART;
import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AuditService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.ScalingActivityService;
import com.sequenceiq.periscope.service.UsageReportingService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;

@ExtendWith(MockitoExtension.class)
public class ScalingRequestTest {

    private static final int TEST_CLUSTER_MAX_NODE_COUNT = 400;

    private static final String TEST_MESSAGE = "test message";

    private static final String TEST_INSTANCEID_PREFIX = "i-";

    private static final AtomicLong ACTIVITY_ID_GENERATOR = new AtomicLong(0L);

    @Mock
    private CloudbreakInternalCrnClient cloudbreakCrnClient;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private LimitsConfigurationService limitsConfigurationService;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private RequestLogging requestLogging;

    @Mock
    private Cluster cluster;

    @Mock
    private ScalingPolicy scalingPolicy;

    @Mock
    private PeriscopeMetricService metricService;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Mock
    private AutoscaleV4Endpoint autoscaleV4Endpoint;

    @Mock
    private HistoryService historyService;

    @Mock
    private HttpNotificationSender notificationSender;

    @Mock
    private AuditService auditService;

    @Mock
    private UsageReportingService usageReportingService;

    @Captor
    private ArgumentCaptor<UpdateStackV4Request> updateStackJsonCaptor;

    @Captor
    private ArgumentCaptor<List<String>> decomissionNodeIdsCaptor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(cloudbreakCommunicator, "cloudbreakInternalCrnClient", cloudbreakCrnClient);
        ReflectionTestUtils.setField(cloudbreakCommunicator, "requestLogging", requestLogging);
    }

    public static Stream<Arguments> scaleUpNodeCountTesting() {
        return Stream.of(
                //TestCase, ExistingClusterNodeCount, existingHostGroupNodeCount,desiredHostGroupNodeCount,expectedScalingAdjustment
                Arguments.of("SCALE_UP", 20, 5, 10, 5),
                Arguments.of("SCALE_UP", 20, 10, 40, 30),
                Arguments.of("SCALE_UP", 20, 30, 100, 70),
                Arguments.of("SCALE_UP", 20, 100, 140, 40),

                Arguments.of("SCALE_UP_AT_MAX", 400, 100, 140, 0),
                Arguments.of("SCALE_UP_AT_MAX", 300, 50, 200, 100),
                Arguments.of("SCALE_UP_AT_MAX", 398, 50, 52, 2),
                Arguments.of("SCALE_UP_AT_MAX", 399, 50, 52, 1)
        );
    }

    @ParameterizedTest(name = "{0}: With existingClusterNodeCount={1}, existingHostGroupNodeCount={2}, desiredHostGroupNodeCount ={3}, expectedNodeCount={4} ")
    @MethodSource("scaleUpNodeCountTesting")
    public void testScaleUpNodeCount(String testCase, int existingClusterNodeCount, int existingHostGroupNodeCount,
            int desiredHostGroupNodeCount, int expectedNodeCount) {

        if (expectedNodeCount > 0) {
            setupMocks();
            when(limitsConfigurationService.getMaxNodeCountLimit(anyString())).thenReturn(TEST_CLUSTER_MAX_NODE_COUNT);
            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount, existingHostGroupNodeCount, existingHostGroupNodeCount,
                    desiredHostGroupNodeCount, Collections.emptyList(), REGULAR);
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).putStack(anyString(), anyString(), updateStackJsonCaptor.capture());
            UpdateStackV4Request request = updateStackJsonCaptor.getValue();
            assertEquals(expectedNodeCount, request.getInstanceGroupAdjustment().getScalingAdjustment().intValue(), "Upscale nodecount should match");
        } else {
            verify(autoscaleV4Endpoint, times(0)).putStack(anyString(), anyString(), updateStackJsonCaptor.capture());
        }
    }

    public static Stream<Arguments> scaleUpNodeCountTestingForStopStart() {
        return Stream.of(
                //TestCase,ExistingClusterNodeCount,runningHostGroupNodeCount,stoppedHostGroupNodeCount,desiredHostGroupNodeCount,expectedScalingAdjustment
                Arguments.of("SCALE_UP", 21, 3, 15, 10, 7),
                Arguments.of("SCALE_UP", 52, 6, 43, 40, 34),
                Arguments.of("SCALE_UP", 25, 19, 3, 20, 1),
                Arguments.of("SCALE_UP", 160, 12, 145, 100, 88),
                Arguments.of("SCALE_UP", 103, 97, 60, 140, 43),

                Arguments.of("SCALE_UP_AT_MAX", 400, 73, 27, 140, 0),
                Arguments.of("SCALE_UP_AT_MAX", 300, 16, 34, 200, 100),
                Arguments.of("SCALE_UP_AT_MAX", 398, 29, 11, 52, 2),
                Arguments.of("SCALE_UP_AT_MAX", 399, 43, 7, 52, 1)
        );
    }

    @ParameterizedTest(name = "{0}: With existingClusterNodeCount={1}, existingHostGroupNodeCount={2}, desiredHostGroupNodeCount ={3}, expectedNodeCount={4} ")
    @MethodSource("scaleUpNodeCountTestingForStopStart")
    public void testScaleUpNodeCountWithStopStartScaling(String testCaseName, int existingClusterNodeCount, int runningHostGroupNodeCount,
            int stoppedHostGroupNodeCount, int desiredHostGroupNodeCount, int expectedNodeCount) {

        if (expectedNodeCount > 0) {
            setupMocks();
            when(limitsConfigurationService.getMaxNodeCountLimit(anyString())).thenReturn(TEST_CLUSTER_MAX_NODE_COUNT);
            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount,
                    runningHostGroupNodeCount + stoppedHostGroupNodeCount, runningHostGroupNodeCount, desiredHostGroupNodeCount,
                    Collections.emptyList(), STOPSTART);
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).putStackStartInstancesByCrn(anyString(), updateStackJsonCaptor.capture());
            UpdateStackV4Request request = updateStackJsonCaptor.getValue();
            assertEquals(expectedNodeCount, request.getInstanceGroupAdjustment().getScalingAdjustment().intValue(), "Upscale nodecount should match");
        } else {
            verify(autoscaleV4Endpoint, times(0)).putStackStartInstancesByCrn(anyString(), updateStackJsonCaptor.capture());
        }
    }

    public static Stream<Arguments> scaleDownNodeCountTesting() {
        return Stream.of(
                // TestCase, ExistingClusterNodeCount, ExistingHostGroupNodeCount, DesiredHostGroupNodeCount, ExpectedScalingScalingAdjustment
                Arguments.of("SCALE_DOWN_1", 25, 11, 4, -7),
                Arguments.of("SCALE_DOWN_2", 70, 28, 15, -13),
                Arguments.of("SCLAE_DOWN_3", 100, 46, 45, -1),
                Arguments.of("NO_SCALE_DOWN_1", 125, 115, 115, 0),
                Arguments.of("NO_SCALE_DOWN_2", 40, 37, 37, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("scaleDownNodeCountTesting")
    void testScaleDownNodeCount(String testCase, int existingClusterNodeCount, int existingHostGroupNodeCount, int desiredHostGroupNodeCount,
            int expectedScalingAdjustment) {

        if (expectedScalingAdjustment < 0) {
            setupMocks();
            List<String> decommissionNodeIds = getNodeIds(abs(expectedScalingAdjustment));
            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount, existingHostGroupNodeCount, existingHostGroupNodeCount,
                    desiredHostGroupNodeCount, decommissionNodeIds, REGULAR);
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).decommissionInternalInstancesForClusterCrn(anyString(),
                    decomissionNodeIdsCaptor.capture(), eq(false));
            List<String> result = decomissionNodeIdsCaptor.getValue();
            assertThat(result).hasSameElementsAs(decommissionNodeIds);
        } else {
            verifyNoInteractions(autoscaleV4Endpoint);
        }
    }

    public static Stream<Arguments> scaleDownNodeCountTestingForStopStart() {
        return Stream.of(
                // TestCase, ExistingClusterNodeCount, RunningHostGroupNodeCount, StoppedHostGroupNodeCount, DesiredHostGroupNodeCount,
                // ExpectedDownScalingAdjustment
                Arguments.of("SCALE_DOWN_1", 27, 13, 10, 5, -8),
                Arguments.of("SCALE_DOWN_2", 47, 40, 0, 29, -11),
                Arguments.of("SCALE_DOWN_3", 150, 100, 20, 20, -80),
                Arguments.of("NO_SCALE_DOWN_1", 29, 10, 23, 10, 0),
                Arguments.of("NO_SCALE_DOWN_2", 40, 30, 4, 37, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("scaleDownNodeCountTestingForStopStart")
    void testScaleDownNodeCountWithStopStart(String testcase, int existingClusterNodeCount, int runningHostGroupNodeCount, int stoppedHostGroupNodeCount,
            int desiredHostGroupNodeCount, int expectedScalingAdjustment) {

        if (expectedScalingAdjustment < 0) {
            setupMocks();
            List<String> decommissionNodeIds = getNodeIds(abs(expectedScalingAdjustment));

            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount, runningHostGroupNodeCount
                            + stoppedHostGroupNodeCount, runningHostGroupNodeCount, desiredHostGroupNodeCount, decommissionNodeIds, STOPSTART);
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).stopInstancesForClusterCrn(anyString(),
                    decomissionNodeIdsCaptor.capture(), eq(false), eq(ScalingStrategy.STOPSTART));
            List<String> result = decomissionNodeIdsCaptor.getValue();
            assertThat(result).hasSameElementsAs(decommissionNodeIds);
        } else {
            verifyNoInteractions(autoscaleV4Endpoint);
        }
    }

    private void setupMocks() {
        lenient().when(scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(anyInt())).thenReturn(false);
        ClusterPertain cluterPertain = mock(ClusterPertain.class);
        when(cluster.getClusterPertain()).thenReturn(cluterPertain);
        when(cluster.getStackCrn()).thenReturn("crn:cdp:datahub:us-west-1:accid:cluster:cluster");
        when(cluterPertain.getTenant()).thenReturn("testTenant");
        lenient().when(cluterPertain.getUserId()).thenReturn("userId");

        BaseAlert baseAlert = mock(BaseAlert.class);
        when(baseAlert.getAlertType()).thenReturn(AlertType.LOAD);
        when(scalingPolicy.getAlert()).thenReturn(baseAlert);
        when(scalingPolicy.getHostGroup()).thenReturn("compute");

        lenient().doNothing().when(metricService).incrementMetricCounter(MetricType.CLUSTER_UPSCALE_TRIGGERED);
        lenient().doNothing().when(metricService).incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        lenient().doNothing().when(metricService).recordScalingAtivityDuration(any(Cluster.class), anyLong());
        CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints = mock(CloudbreakServiceCrnEndpoints.class);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(cloudbreakCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakMessagesService.getMessage(anyString(), any(List.class))).thenReturn(TEST_MESSAGE);
        when(cloudbreakMessagesService.getMessageWithArgs(anyString(), any())).thenReturn(TEST_MESSAGE);

        ScalingActivity scalingActivity = mock(ScalingActivity.class);
        when(scalingActivityService.update(anyLong(), any(FlowIdentifier.class), any(ActivityStatus.class), anyString())).thenReturn(scalingActivity);

        lenient().doCallRealMethod().when(requestLogging).logResponseTime(any(), anyString());
        lenient().doCallRealMethod().when(cloudbreakCommunicator).putStackForCluster(any(Cluster.class), any(UpdateStackV4Request.class));
        lenient().doCallRealMethod().when(cloudbreakCommunicator).decommissionInstancesForCluster(any(Cluster.class), anyList());
        lenient().doCallRealMethod().when(cloudbreakCommunicator).putStackStartInstancesForCluster(any(Cluster.class), any(UpdateStackV4Request.class));
        lenient().doCallRealMethod().when(cloudbreakCommunicator).stopInstancesForCluster(any(Cluster.class), anyList());
    }

    private List<String> getNodeIds(int count) {
        return IntStream.range(0, count).boxed().map(i -> TEST_INSTANCEID_PREFIX + i).collect(Collectors.toList());
    }

    private ScalingRequest initializeTestRequest(int existingClusterNodeCount, int hostGroupNodeCount, int servicesHealthyHostGroupNodeCount,
            int desiredHostGroupNodeCount, List<String> decommissionNodeId, ScalingAdjustmentType adjustmentType) {
        ScalingRequest scalingRequest = new ScalingRequest(cluster, scalingPolicy, existingClusterNodeCount,
                hostGroupNodeCount, desiredHostGroupNodeCount, decommissionNodeId, servicesHealthyHostGroupNodeCount, adjustmentType,
                ACTIVITY_ID_GENERATOR.incrementAndGet());
        scalingRequest.setMetricService(metricService);
        scalingRequest.setScalingHardLimitsService(scalingHardLimitsService);
        scalingRequest.setCloudbreakInternalCrnClient(cloudbreakCrnClient);
        scalingRequest.setLimitsConfigurationService(limitsConfigurationService);
        scalingRequest.setCloudbreakMessagesService(cloudbreakMessagesService);
        scalingRequest.setHistoryService(historyService);
        scalingRequest.setHttpNotificationSender(notificationSender);
        scalingRequest.setAuditService(auditService);
        scalingRequest.setUsageReportingService(usageReportingService);
        scalingRequest.setCloudbreakCommunicator(cloudbreakCommunicator);
        scalingRequest.setScalingActivityService(scalingActivityService);
        return scalingRequest;
    }
}
