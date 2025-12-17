package com.sequenceiq.periscope.endpointtests;

import static com.sequenceiq.periscope.api.model.AdjustmentType.LOAD_BASED;
import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.ResourceAuthorizationService;
import com.sequenceiq.authorization.service.ResourceNameFactoryService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsClient;
import com.sequenceiq.cloudbreak.quartz.configuration.QuartzJobInitializer;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ClusterState;
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
import com.sequenceiq.periscope.service.NodeDeletionService;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;
import com.sequenceiq.periscope.testcontext.EndpointTestContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = EndpointTestContext.class)
@ActiveProfiles("test")
class DistroXAutoScaleClusterV1EndpointTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/as";

    private static final Long TEST_USER_ID = 100L;

    private static final Long TEST_USER_ID_2 = 101L;

    private static final Long TEST_WORKSPACE_ID = 100L;

    private static final Long TEST_WORKSPACE_ID_2 = 101L;

    private static final Integer MIN_RESOURCE_COUNT = 4;

    private static final Integer MAX_RESOURCE_COUNT = 15;

    private static final Integer COOL_DOWN_MINUTES = 5;

    private static final String TEST_SCHEDULE_CRON = "1 0 1 1 1 1";

    private static final String TEST_SCHEDULE_TIMEZONE = "GMT";

    private static final Integer TEST_SCHEDULE_NODE_COUNT = 10;

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_ACCOUNT_ID_2 = "accid2";

    private static final String TEST_CLUSTER_NAME = "testCluster";

    private static final String TEST_CLUSTER_NAME_2 = "testCluster2";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_USER_CRN_2 = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID_2);

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_CLUSTER_CRN_2 = String.format("crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com", TEST_ACCOUNT_ID_2);

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
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @MockBean
    private NodeDeletionService nodeDeletionService;

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
    private ClusterPertainRepository clusterPertainRepository;

    @Inject
    private LoadAlertRepository loadAlertRepository;

    @Inject
    private TimeAlertRepository timeAlertRepository;

    private DistroXAutoScaleClusterV1Endpoint distroXAutoScaleClusterV1Endpoint;

    private DistroXAutoScaleClusterV1Endpoint distroXAutoScaleClusterV1Endpoint2;

    @BeforeEach
    void setup() {
        Cluster cluster1 = createTestCluster(TEST_CLUSTER_CRN, TEST_CLUSTER_NAME, TEST_ACCOUNT_ID, TEST_WORKSPACE_ID,
                TEST_USER_ID.toString(), TEST_USER_CRN, "cluster1");

        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setCrn(TEST_USER_CRN).setEmail("dummyuser@cloudera.com").setUserId(TEST_USER_ID.toString()).build();
        UserManagementProto.Account account = UserManagementProto.Account.newBuilder()
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_GCP_AUTOSCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AWS_STOP_START_SCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_AZURE_STOP_START_SCALING").build())
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_GCP_STOP_START_SCALING").build())
                .build();

        when(grpcUmsClient.getUserDetails(eq(TEST_USER_CRN))).thenReturn(user);
        when(grpcUmsClient.getAccountDetails(eq(TEST_ACCOUNT_ID))).thenReturn(account);
        doNothing().when(resourceAuthorizationService).authorize(eq("crn:cdp:iam:us-west-1:accid:cluster:mockuser@cloudera.com"), any(), any());
        distroXAutoScaleClusterV1Endpoint = new AutoscaleUserCrnClientBuilder(String.format(SERVICE_ADDRESS, port))
                .build().withCrn(TEST_USER_CRN).distroXAutoScaleClusterV1Endpoint();
        UserManagementProto.User user2 = UserManagementProto.User.newBuilder()
                .setCrn(TEST_USER_CRN_2).setEmail("dummyuser@cloudera.com").setUserId(TEST_USER_ID_2.toString()).build();


        Cluster cluster2 = createTestCluster(TEST_CLUSTER_CRN_2, TEST_CLUSTER_NAME_2, TEST_ACCOUNT_ID_2, TEST_WORKSPACE_ID_2,
                TEST_USER_ID_2.toString(), TEST_USER_CRN_2, "cluster2");

        UserManagementProto.Account account2 = UserManagementProto.Account.newBuilder()
                .addEntitlements(UserManagementProto.Entitlement.newBuilder().setEntitlementName("DATAHUB_GCP_AUTOSCALING").build())
                .build();
        when(grpcUmsClient.getUserDetails(eq(TEST_USER_CRN_2))).thenReturn(user2);
        when(grpcUmsClient.getAccountDetails(eq(TEST_ACCOUNT_ID_2))).thenReturn(account2);
        doNothing().when(resourceAuthorizationService).authorize(eq("crn:cdp:iam:us-west-1:accid3:cluster:mockuser3@cloudera.com"), any(), any());
        doNothing().when(resourceAuthorizationService).authorize(eq("crn:cdp:iam:us-west-1:accid2:cluster:mockuser2@cloudera.com"), any(), any());
        distroXAutoScaleClusterV1Endpoint2 = new AutoscaleUserCrnClientBuilder(String.format(SERVICE_ADDRESS, port))
                .build().withCrn(TEST_USER_CRN_2).distroXAutoScaleClusterV1Endpoint();

        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("http://clusterproxy"));
        when(limitsConfigurationService.getMaxNodeCountLimit(anyString())).thenReturn(400);
        when(recommendationService.getAutoscaleRecommendations(any()))
                .thenReturn(new AutoscaleRecommendationV4Response(Set.of("compute"), Set.of("compute")));
        doNothing().when(nodeDeletionService).deleteStoppedNodesIfPresent(any(), anyString());
    }

    private Cluster createTestCluster(String clusterCrn, String clusterName, String accountId, long workspaceId,
            String userId, String userCrn, String bluePrintText) {
        Cluster testCluster = new Cluster();
        testCluster.setStackCrn(clusterCrn);
        testCluster.setStackName(clusterName);
        testCluster.setCloudPlatform("AWS");
        testCluster.setBluePrintText(bluePrintText);
        testCluster.setState(ClusterState.RUNNING);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(accountId);
        clusterPertain.setWorkspaceId(workspaceId);
        clusterPertain.setUserId(userId);
        clusterPertain.setUserCrn(userCrn);
                testCluster.setClusterPertain(clusterPertainRepository.findFirstByUserCrn(clusterPertain.getUserCrn())
                        .orElseGet(() -> clusterPertainRepository.save(clusterPertain)));
        clusterRepository.save(testCluster);
        return testCluster;
    }

    @AfterEach
    void tearDown() {
        loadAlertRepository.deleteAll();
        timeAlertRepository.deleteAll();
        clusterRepository.deleteAll();
        clusterPertainRepository.deleteAll();
    }

    @Test
    void testUpdateAutoscaleConfigByClusterCrnForEnableDisableAutoscaling() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");

        distroXAutoscaleClusterRequest.setEnableAutoscaling(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertFalse(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be disabled");
    }

    @Test
    void testEnableAutoscaleForClusterCrn() {
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enable());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.disable());
        assertFalse(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be disabled");

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enable());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
    }

    @Test
    void testDisableAutoscaleForClusterCrn() {
        Cluster cluster = clusterRepository.findByStackNameAndTenant(TEST_CLUSTER_NAME, TEST_ACCOUNT_ID).get();
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enable());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
        Assertions.assertEquals(ClusterState.RUNNING, xAutoscaleClusterResponse.getState());

        cluster.setState(ClusterState.SUSPENDED);
        clusterRepository.save(cluster);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.disable());
        assertFalse(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be disabled");
        Assertions.assertEquals(ClusterState.PENDING, xAutoscaleClusterResponse.getState());
    }

    @Test
    void testEnableAutoscaleForClusterName() {
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.enable());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disable());
        assertFalse(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be disabled");
    }

    @Test
    void testEnableStopStartScalingForClusterName() {
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.enableStopStart());
        assertFalse(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        clusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disableStopStart());
        assertFalse(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testEnableStopStartScalingForClusterCrn() {
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.enableStopStart());
        assertFalse(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        clusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterCrn(TEST_CLUSTER_CRN, AutoscaleClusterState.disableStopStart());
        assertFalse(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testUpdateAutoscaleConfigByClusterCrnForEnableDisableStopStartScaling() {
        DistroXAutoscaleClusterRequest clusterRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        clusterRequest.setLoadAlertRequests(loadAlertRequests);
        clusterRequest.setEnableAutoscaling(true);

        clusterRequest.setUseStopStartMechanism(null);
        DistroXAutoscaleClusterResponse clusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertTrue(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        clusterRequest.setUseStopStartMechanism(true);
        clusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertTrue(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        clusterRequest.setUseStopStartMechanism(false);
        clusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, clusterRequest);
        assertFalse(clusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disable");
    }

    @Test
    void testUpdateAutoscaleConfigByClusterCrnForLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 1, xAutoscaleClusterResponse.getLoadAlerts().size());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
        xAutoscaleClusterResponse.getLoadAlerts().stream().forEach(this::validateLoadAlertResponse);
    }

    @Test
    void testUpdateAutoscaleConfigByClusterNameForLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 1, xAutoscaleClusterResponse.getLoadAlerts().size());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
        xAutoscaleClusterResponse.getLoadAlerts().stream().forEach(this::validateLoadAlertResponse);
    }

    @Test
    void testDeleteAlertByClusterCrnForLoadAlerts() {
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
    void testUpdateAutoscaleConfigByClusterCrnForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(false);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
        xAutoscaleClusterResponse.getTimeAlerts().stream().forEach(this::validateTimeAlertResponse);
    }

    @Test
    void testUpdateAutoscaleConfigByClusterNameForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(false);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);

        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());
        assertTrue(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling should be enabled");
        xAutoscaleClusterResponse.getTimeAlerts().stream().forEach(this::validateTimeAlertResponse);
    }

    @Test
    void testDeleteAlertByClusterNameForTimeAlerts() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(false);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse =
                distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);
        assertEquals("Retrieved Alerts Size Should Match", 2, xAutoscaleClusterResponse.getTimeAlerts().size());

        distroXAutoScaleClusterV1Endpoint.deleteAlertsForClusterName(TEST_CLUSTER_NAME);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.getClusterByName(TEST_CLUSTER_NAME);
        assertEquals("Retrieved Alerts Size Should Match", 0, xAutoscaleClusterResponse.getTimeAlerts().size());
    }

    @Test
    void testUpdateAutoscaleConfigWhenMultipleAlertTypes() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("group1", "group2"));

        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint
                        .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest));
    }

    @Test
    void testUpdateAutoscaleConfigByClusterCrnForMultipleLoadAlerts() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("compute", "compute"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint
                        .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest));
    }

    @Test
    void testUpdateAutoscaleConfigByClusterCrnForMultipleLoadAlertsAndHostGroups() {
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(2, List.of("group1", "group2"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);

        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint
                        .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest));
    }

    @Test
    void testUpdateAutoscaleConfigWithTimeAlertsAndStopStartEnabled() {
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);

        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest));
    }

    @Test
    void testDisableStopStartScalingViaStateUpdateWithPreExistingTimeAlerts() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(false);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);

        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testEnableStopStartScalingViaStateUpdateWithPreExistingTimeAlerts() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest));
    }

    @Test
    void testEnableStopStartScalingViaConfigUpdateWithPreExistingTimeAlerts() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(false);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);
        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest));
    }

    @Test
    void testCreateTimeAlertWithPreExistingStopStartLoadAlert() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(true);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);

        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testCreateTimeAlertAfterDisablingStopStartAutoscaling() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(true);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disableStopStart());
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);

        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testCreateTimeAlertAfterDisablingAutoscaling() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(true);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        AutoscaleClusterState autoscaleClusterState = new AutoscaleClusterState();
        autoscaleClusterState.setUseStopStartMechanism(false);
        autoscaleClusterState.setEnableAutoscaling(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, autoscaleClusterState);
        assertFalse(xAutoscaleClusterResponse.isAutoscalingEnabled(), "Autoscaling scaling should be disabled");
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(false);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);

        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testEnableStopStartAfterDisablingAutoScaling() {
        DistroXAutoscaleClusterRequest distroXAutoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequests);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterCrn(TEST_CLUSTER_CRN, distroXAutoscaleClusterRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.enableAutoscaleForClusterName(TEST_CLUSTER_NAME, AutoscaleClusterState.disable());
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        List<TimeAlertRequest> timeAlertRequest = getTimeAlertRequests(0, List.of());
        distroXAutoscaleClusterRequest.setTimeAlertRequests(timeAlertRequest);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        distroXAutoscaleClusterRequest.setLoadAlertRequests(loadAlertRequests);
        distroXAutoscaleClusterRequest.setEnableAutoscaling(true);
        distroXAutoscaleClusterRequest.setUseStopStartMechanism(true);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, distroXAutoscaleClusterRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");
    }

    @Test
    void testDefaultForStopStartEntitlementEnabledLoadPolicy() {
        // Load policy. stop start not configured. Entitlement enabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");
    }

    @Test
    void testDefaultForStopStartEntitlementEnabledSchedulePolicy() {
        // Schedule policy. stop start not configured. Entitlement Enabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(1, List.of("compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testDefaultForStopStartEntitlementDisabledLoadPolicy() {
        // Load policy. stop start not configured. Entitlement disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint2
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testDefaultForStopStartEnabledEntitlementDisabledLoadPolicy() {
        // Load policy. stop start set to true. Entitlement disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        autoscaleRequest.setUseStopStartMechanism(true);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint2.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest));
    }

    @Test
    void testDefaultForStopStartEnabledEntitlementDisabledSchedulePolicy() {
        // Schedule policy. stop start set to true. Entitlement disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        autoscaleRequest.setUseStopStartMechanism(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(1, List.of("compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        Assertions.assertThrows(BadRequestException.class,
                () -> distroXAutoScaleClusterV1Endpoint2.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest));
    }

    @Test
    void testDefaultForStopStartDisabledEntitlementEnabledLoadPolicy() {
        // Load policy. stop start set to false. Entitlement enabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        autoscaleRequest.setUseStopStartMechanism(false);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testDefaultForStopStartDisabledEntitlementDisabledSchedulePolicy() {
        // Schedule policy. stop start set to false. Entitlement disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(1, List.of("compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint2
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testDefaultForStopStartDisabledEntitlementDisabledLoadPolicy() {
        // Load policy. stop start set to false. Entitlement disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        autoscaleRequest.setUseStopStartMechanism(false);
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint2
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testDefaultForStopStartEntitlementDisabledSchedulePolicy() {
        // Schedule policy. stop start not configured. Entitlement Disabled.
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(true);
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(1, List.of("compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint2
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME_2, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testCreateLoadAlertAfterDisablingTimeAlertsInSingleRequest() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        autoscaleRequest.setEnableAutoscaling(true);
        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disbled");

        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setTimeAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");
    }

    @Test
    void testCreateTimeAlertAfterDisablingLoadAlertsInSingleRequest() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(null);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);

        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
    }

    @Test
    void testCreateLoadAlertAfterDisablingTimeAlerts() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        autoscaleRequest.setTimeAlertRequests(null);
        autoscaleRequest.setEnableAutoscaling(false);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(true);
        autoscaleRequest.setEnableAutoscaling(true);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");
    }

    @Test
    void testCreateTimeAlertAfterDisablingLoadAlerts() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        List<LoadAlertRequest> loadAlertRequests = getLoadAlertRequests(1, List.of("compute"));
        autoscaleRequest.setLoadAlertRequests(loadAlertRequests);
        autoscaleRequest.setUseStopStartMechanism(null);
        autoscaleRequest.setEnableAutoscaling(true);

        DistroXAutoscaleClusterResponse xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint
                .updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertTrue(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be enabled");

        autoscaleRequest.setTimeAlertRequests(null);
        autoscaleRequest.setEnableAutoscaling(false);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");

        List<TimeAlertRequest> timeAlertRequests = getTimeAlertRequests(2, List.of("compute", "compute"));
        autoscaleRequest.setTimeAlertRequests(timeAlertRequests);
        autoscaleRequest.setLoadAlertRequests(null);
        autoscaleRequest.setUseStopStartMechanism(null);
        xAutoscaleClusterResponse = distroXAutoScaleClusterV1Endpoint.updateAutoscaleConfigByClusterName(TEST_CLUSTER_NAME, autoscaleRequest);
        assertFalse(xAutoscaleClusterResponse.isStopStartScalingEnabled(), "StopStart scaling should be disabled");
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
