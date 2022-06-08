package com.sequenceiq.periscope.endpointtests;

import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.ResourceAuthorizationService;
import com.sequenceiq.authorization.service.ResourceNameFactoryService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsClient;
import com.sequenceiq.cloudbreak.quartz.configuration.QuartzJobInitializer;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.LoadAlertResponse;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.api.model.TimeAlertResponse;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClientBuilder;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.repository.ClusterPertainRepository;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.LoadAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;
import com.sequenceiq.periscope.testcontext.EndpointTestContext;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = EndpointTestContext.class)
@ActiveProfiles("test")
public class DistroXAutoScaleClusterV1EndpointTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/as";

    private static final Long TEST_USER_ID = 100L;

    private static final Long TEST_WORKSPACE_ID = 100L;

    private static final Integer MIN_RESOURCE_COUNT = 4;

    private static final Integer MAX_RESOURCE_COUNT = 15;

    private static final Integer COOL_DOWN_MINUTES = 5;

    private static final String TEST_SCHEDULE_CRON = "1 0 1 1 1 1";

    private static final String TEST_SCHEDULE_TIMEZONE = "GMT";

    private static final Integer TEST_SCHEDULE_NODE_COUNT = 10;

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_CLUSTER_NAME = "testCluster";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @LocalServerPort
    private int port;

    @MockBean
    private ResourceAuthorizationService resourceAuthorizationService;

    @MockBean
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @MockBean
    private LimitsConfigurationService limitsConfigurationService;

    @MockBean
    private AutoscaleRecommendationService recommendationService;

    @MockBean
    private QuartzJobInitializer quartzJobInitializer;

    @MockBean(name = "grpcUmsClient")
    private GrpcUmsClient grpcUmsClient;

    @MockBean
    private OwnerAssignmentService ownerAssignmentService;

    @MockBean(name = "umsClient")
    private UmsClient umsClient;

    @MockBean(name = "resourceNameFactoryService")
    private ResourceNameFactoryService resourceNameFactoryService;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ClusterPertainRepository clusterPertainRepsitory;

    @Inject
    private LoadAlertRepository loadAlertRepository;

    @Inject
    private TimeAlertRepository timeAlertRepository;

    private DistroXAutoScaleClusterV1Endpoint distroXAutoScaleClusterV1Endpoint;

    @Before
    public void setup() {
        distroXAutoScaleClusterV1Endpoint = new AutoscaleUserCrnClientBuilder(String.format(SERVICE_ADDRESS, port))
                .build().withCrn(TEST_USER_CRN).distroXAutoScaleClusterV1Endpoint();

        Cluster testCluster = new Cluster();
        testCluster.setStackCrn(TEST_CLUSTER_CRN);
        testCluster.setStackName(TEST_CLUSTER_NAME);
        testCluster.setCloudPlatform("AWS");

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_ACCOUNT_ID);
        clusterPertain.setWorkspaceId(TEST_WORKSPACE_ID);
        clusterPertain.setUserId(TEST_USER_ID.toString());
        clusterPertain.setUserCrn(TEST_USER_CRN);

        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setCrn(TEST_USER_CRN).setEmail("dummyuser@cloudera.com").setUserId(TEST_USER_ID.toString()).build();
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AWS_AUTOSCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AZURE_AUTOSCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_GCP_AUTOSCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AWS_STOP_START_SCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AZURE_STOP_START_SCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_GCP_STOP_START_SCALING").build())
                .build();
        testCluster.setClusterPertain(
                clusterPertainRepsitory.findByUserCrn(clusterPertain.getUserCrn())
                        .orElseGet(() -> clusterPertainRepsitory.save(clusterPertain)));
        clusterRepository.save(testCluster);

        when(grpcUmsClient.getUserDetails(anyString(), any())).thenReturn(user);
        when(grpcUmsClient.getAccountDetails(anyString(), any())).thenReturn(account);
        doNothing().when(resourceAuthorizationService).authorize(eq("crn:cdp:iam:us-west-1:accid:cluster:mockuser@cloudera.com"), any(), any());
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("http://clusterproxy"));
        when(limitsConfigurationService.getMaxNodeCountLimit()).thenReturn(400);
        when(recommendationService.getAutoscaleRecommendations(TEST_CLUSTER_CRN))
                .thenReturn(new AutoscaleRecommendationV4Response(Set.of("compute"), Set.of("compute")));
    }

    @After
    public void tearDown() {
        loadAlertRepository.deleteAll();
        timeAlertRepository.deleteAll();
        clusterRepository.deleteAll();
        clusterPertainRepsitory.deleteAll();
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterCrnForEnableDisableAutoscaling() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());

        distroXAutoscaleClusterRequest.setEnableAutoscaling(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertFalse("Autoscaling should be disabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
    }

    @Test
    public void testEnableAutoscaleForClusterCrn() {
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enable());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.disable());
        assertFalse("Autoscaling should be disabled", xAutoscaleClusterResponse.isAutoscalingEnabled());

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enable());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
    }

    @Test
    public void testEnableAutoscaleForClusterName() {
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.enable());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disable());
        assertFalse("Autoscaling should be disabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
    }

    @Test
    public void testEnableStopStartScalingForClusterName() {
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.enableStopStart());
        assertTrue("StopStart scaling should be enabled", clusterResponse.isStopStartScalingEnabled());

        clusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disableStopStart());
        assertFalse("StopStart scaling should be disabled", clusterResponse.isStopStartScalingEnabled());
    }

    @Test
    public void testEnableStopStartScalingForClusterCrn() {
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enableStopStart());
        assertTrue("StopStart scaling should be enabled", clusterResponse.isStopStartScalingEnabled());

        clusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.disableStopStart());
        assertFalse("StopStart scaling should be disabled", clusterResponse.isStopStartScalingEnabled());
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterCrnForEnableDisableStopStartScaling() {
        DistroXAutoscaleClusterRequest clusterRequest = new DistroXAutoscaleClusterRequest();
        clusterRequest.setEnableAutoscaling(true);

        clusterRequest.setUseStopStartMechanism(null);
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertFalse("StopStart scaling should be disabled if not specified in reuqest", clusterResponse.isStopStartScalingEnabled());

        clusterRequest.setUseStopStartMechanism(true);
        clusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertTrue("StopStart scaling should be enabled", clusterResponse.isStopStartScalingEnabled());

        clusterRequest.setUseStopStartMechanism(false);
        clusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertFalse("StopStart scaling should be disabled", clusterResponse.isStopStartScalingEnabled());
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterCrnForLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 1, xAutoscaleClusterResponse.getLoadAlerts().size());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
        xAutoscaleClusterResponse.getLoadAlerts().stream().forEach(this::validateLoadAlertResponse);
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterNameForLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 1, xAutoscaleClusterResponse.getLoadAlerts().size());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
        xAutoscaleClusterResponse.getLoadAlerts().stream().forEach(this::validateLoadAlertResponse);
    }

    @Test
    public void testDeleteAlertByClusterCrnForLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 1, xAutoscaleClusterResponse.getLoadAlerts().size());
        distroXAutoScaleClusterV1Endpoint.deleteAlertsForClusterCrn(TEST_CLUSTER_CRN);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.getClusterByCrn(TEST_CLUSTER_CRN);
        assertEquals("Retrieved Alerts Size Should Match", 0, xAutoscaleClusterResponse.getLoadAlerts().size());
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterCrnForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
        xAutoscaleClusterResponse.getTimeAlerts().stream().forEach(this::validateTimeAlertResponse);
    }

    @Test
    public void testUpdateAutoscaleConfigByClusterNameForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());
        assertTrue("Autoscaling should be enabled", xAutoscaleClusterResponse.isAutoscalingEnabled());
        xAutoscaleClusterResponse.getTimeAlerts().stream().forEach(this::validateTimeAlertResponse);
    }

    @Test
    public void testDeleteAlertByClusterNameForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);
        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());

        distroXAutoScaleClusterV1Endpoint.deleteAlertsForClusterName(TEST_CLUSTER_NAME);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.getClusterByName(TEST_CLUSTER_NAME);
        assertEquals("Retrieved Alerts Size Should Match", 0, xAutoscaleClusterResponse.getTimeAlerts().size());
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateAutoscaleConfigWhenMultipleAlertTypes() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("group1", "group2"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateAutoscaleConfigByClusterCrnForMultipleLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("compute", "compute"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateAutoscaleConfigByClusterCrnForMultipleLoadAlertsAndHostGroups() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("group1", "group2"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testUpdateAutoscaleConfigWithTimeAlertsAndStopStartEnabled() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testEnableStopStartScalingViaStateUpdateWithPreExistingTimeAlerts() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enableStopStart());
    }

    @Test(expected = BadRequestException.class)
    public void testEnableStopStartScalingViaConfigUpdateWithPreExistingTimeAlerts() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        distroXAutoscaleClusterRequest.setTimeAlertRequests(null);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateTimeAlertWithStopStartAlreadyEnabled() {
        distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enableStopStart());

        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setEnableAutoscaling(true);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateTimeAlertWithPreExistingStopStartLoadAlert() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(true);
        autoscaleRequest.setEnableAutoscaling(true);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);

        distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
    }

    private void validateLoadAlertResponse(LoadAlertResponse alertResponse) {
        assertEquals("Retrieved HostGroup Should Match", "compute", alertResponse.getScalingPolicy().getHostGroup());
        assertEquals("Retrieved AdjustmentType Should Match", LOAD_BASED, alertResponse.getScalingPolicy().getAdjustmentType());
        assertEquals("Retrieved MinResourceValue Should Match", MIN_RESOURCE_COUNT, alertResponse.getLoadAlertConfiguration().getMinResourceValue());
        assertEquals("Retrieved MaxResourceValue Should Match", MAX_RESOURCE_COUNT, alertResponse.getLoadAlertConfiguration().getMaxResourceValue());
        assertEquals("Retrieved CoolDownMins Should Match", COOL_DOWN_MINUTES, alertResponse.getLoadAlertConfiguration().getCoolDownMinutes());
    }

    private void validateTimeAlertResponse(TimeAlertResponse alertResponse) {
        assertEquals("Retrieved HostGroup Should Match", "compute", alertResponse.getScalingPolicy().getHostGroup());
        assertEquals("Retrieved AdjustmentType Should Match", NODE_COUNT, alertResponse.getScalingPolicy().getAdjustmentType());
        assertEquals("Retrieved Adjustment Should Match", TEST_SCHEDULE_NODE_COUNT, alertResponse.getScalingPolicy().getScalingAdjustment());
        assertEquals("Retrieved Cron Should Match", TEST_SCHEDULE_CRON, alertResponse.getCron());
        assertEquals("Retrieved TimeZone Should Match", TEST_SCHEDULE_TIMEZONE, alertResponse.getTimeZone());
    }

    private List<LoadAlertRequest> getLoadAlertRequests(Integer loadAlertRequestCount, List<String> computeGroups) {
        List<LoadAlertRequest> loadRequests = new ArrayList();
        IntStream.range(0, loadAlertRequestCount).forEach(loop -> {
            LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
            loadAlertRequest.setAlertName("testalert");
            LoadAlertConfigurationRequest loadAlertConfiguration = new LoadAlertConfigurationRequest();
            loadAlertConfiguration.setMinResourceValue(MIN_RESOURCE_COUNT);
            loadAlertConfiguration.setMaxResourceValue(MAX_RESOURCE_COUNT);
            loadAlertConfiguration.setCoolDownMinutes(COOL_DOWN_MINUTES);

            ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
            scalingPolicyRequest.setAdjustmentType(LOAD_BASED);
            scalingPolicyRequest.setHostGroup(computeGroups.get(loop));

            loadAlertRequest.setScalingPolicy(scalingPolicyRequest);
            loadAlertRequest.setLoadAlertConfiguration(loadAlertConfiguration);
            loadRequests.add(loadAlertRequest);
        });
        return loadRequests;
    }

    private List<TimeAlertRequest> getTimeAlertRequests(Integer timeAlertRequestCount, List<String> computeGroups) {
        List<TimeAlertRequest> timeAlertRequests = new ArrayList();
        IntStream.range(0, timeAlertRequestCount).forEach(loop -> {
            TimeAlertRequest timeAlertRequest = new TimeAlertRequest();
            timeAlertRequest.setAlertName("testalert");
            timeAlertRequest.setTimeZone(TEST_SCHEDULE_TIMEZONE);
            timeAlertRequest.setCron(TEST_SCHEDULE_CRON);

            ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
            scalingPolicyRequest.setAdjustmentType(NODE_COUNT);
            scalingPolicyRequest.setScalingAdjustment(TEST_SCHEDULE_NODE_COUNT);
            scalingPolicyRequest.setHostGroup(computeGroups.get(loop));

            timeAlertRequest.setScalingPolicy(scalingPolicyRequest);
            timeAlertRequests.add(timeAlertRequest);
        });
        return timeAlertRequests;
    }
}