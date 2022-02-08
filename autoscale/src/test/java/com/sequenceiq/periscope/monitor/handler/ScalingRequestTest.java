package com.sequenceiq.periscope.monitor.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AuditService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.UsageReportingService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;

@ExtendWith(MockitoExtension.class)
public class ScalingRequestTest {

    private static final int TEST_CLUSTER_MAX_NODE_COUNT = 400;

    @Mock
    private CloudbreakInternalCrnClient cloudbreakCrnClient;

    @Mock
    private LimitsConfigurationService limitsConfigurationService;

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

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
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
        ArgumentCaptor<UpdateStackV4Request> captor = ArgumentCaptor.forClass(UpdateStackV4Request.class);

        if (expectedNodeCount > 0) {
            initScaleUpMocks();
            when(cluster.isStopStartScalingEnabled()).thenReturn(false);
            when(limitsConfigurationService.getMaxNodeCountLimit()).thenReturn(TEST_CLUSTER_MAX_NODE_COUNT);
            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount, existingHostGroupNodeCount, desiredHostGroupNodeCount, List.of());
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).putStack(anyString(), anyString(), captor.capture());
            UpdateStackV4Request request = captor.getValue();
            assertEquals("Upscale nodecount should match", expectedNodeCount, request.getInstanceGroupAdjustment().getScalingAdjustment().intValue());
        } else {
            verify(autoscaleV4Endpoint, times(0)).putStack(anyString(), anyString(), captor.capture());
        }
    }

    @ParameterizedTest(name = "{0}: With existingClusterNodeCount={1}, existingHostGroupNodeCount={2}, desiredHostGroupNodeCount ={3}, expectedNodeCount={4} ")
    @MethodSource("scaleUpNodeCountTesting")
    public void testScaleUpNodeCountWithStopStartScaling(String testCaseName, int existingClusterNodeCount, int existingHostGroupNodeCount,
            int desiredHostGroupNodeCount, int expectedNodeCount) {
        ArgumentCaptor<UpdateStackV4Request> captor = ArgumentCaptor.forClass(UpdateStackV4Request.class);

        if (expectedNodeCount > 0) {
            initScaleUpMocks();
            when(cluster.isStopStartScalingEnabled()).thenReturn(true);
            when(limitsConfigurationService.getMaxNodeCountLimit()).thenReturn(TEST_CLUSTER_MAX_NODE_COUNT);
            ScalingRequest scalingRequest = initializeTestRequest(existingClusterNodeCount, existingHostGroupNodeCount, desiredHostGroupNodeCount, List.of());
            scalingRequest.run();

            verify(autoscaleV4Endpoint, times(1)).putStackStartInstancesByCrn(anyString(), captor.capture());
            UpdateStackV4Request request = captor.getValue();
            assertEquals("Upscale nodecount should match", expectedNodeCount, request.getInstanceGroupAdjustment().getScalingAdjustment().intValue());
        } else {
            verify(autoscaleV4Endpoint, times(0)).putStackStartInstancesByCrn(anyString(), captor.capture());
        }
    }

    private void initScaleUpMocks() {
        when(scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(anyInt())).thenReturn(false);
        ClusterPertain cluterPertain = mock(ClusterPertain.class);
        when(cluster.getClusterPertain()).thenReturn(cluterPertain);
        when(cluster.getStackCrn()).thenReturn("testStackCrn");
        when(cluterPertain.getTenant()).thenReturn("testTenant");
        lenient().when(cluterPertain.getUserId()).thenReturn("userId");

        BaseAlert baseAlert = mock(BaseAlert.class);
        when(baseAlert.getAlertType()).thenReturn(AlertType.LOAD);
        when(scalingPolicy.getAlert()).thenReturn(baseAlert);
        when(scalingPolicy.getHostGroup()).thenReturn("compute");

        doNothing().when(metricService).incrementMetricCounter(MetricType.CLUSTER_UPSCALE_TRIGGERED);
        CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints = mock(CloudbreakServiceCrnEndpoints.class);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(cloudbreakCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakMessagesService.getMessage(anyString(), any(List.class))).thenReturn("test");

    }

    private ScalingRequest initializeTestRequest(int existingClusterNodeCount, int hostGroupNodeCount,
            int desiredHostGroupNodeCount, List<String> decommissionNodeId) {
        ScalingRequest scalingRequest = new ScalingRequest(cluster, scalingPolicy, existingClusterNodeCount,
                hostGroupNodeCount, desiredHostGroupNodeCount, decommissionNodeId);
        scalingRequest.setMetricService(metricService);
        scalingRequest.setScalingHardLimitsService(scalingHardLimitsService);
        scalingRequest.setCloudbreakInternalCrnClient(cloudbreakCrnClient);
        scalingRequest.setLimitsConfigurationService(limitsConfigurationService);
        scalingRequest.setCloudbreakMessagesService(cloudbreakMessagesService);
        scalingRequest.setHistoryService(historyService);
        scalingRequest.setHttpNotificationSender(notificationSender);
        scalingRequest.setAuditService(auditService);
        scalingRequest.setUsageReportingService(usageReportingService);
        return scalingRequest;
    }
}
