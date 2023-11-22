package com.sequenceiq.periscope.endpointtests;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.authorization.service.ResourceAuthorizationService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsClient;
import com.sequenceiq.cloudbreak.quartz.configuration.QuartzJobInitializer;
import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleScalingActivityV1Endpoint;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.client.AutoscaleUserCrnClientBuilder;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.repository.ClusterPertainRepository;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;
import com.sequenceiq.periscope.testcontext.EndpointTestContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = EndpointTestContext.class)
@ActiveProfiles("test")
public class DistroXAutoScaleScalingActivityV1EndpointTest {

    private static final String SERVICE_ADDRESS = "http://localhost:%d/as";

    private static final Long TEST_USER_ID = 100L;

    private static final Long TEST_WORKSPACE_ID = 100L;

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_CLUSTER_NAME = "testCluster";

    private static final String TEST_CLUSTER_NAME_NULL = null;

    private static final String TEST_TENANT = "testTenant";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_CLUSTER_CRN_NULL = null;

    private static final String TEST_OPERATION_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String TEST_OPERATION_ID_9 = "9d74eee4-1cad-45d7-b645-7ccf9edbb73l";

    private static final String TEST_OPERATION_ID_NULL = null;

    private static final String TEST_REASON = "test trigger reason";

    @LocalServerPort
    private int port;

    @MockBean
    private ResourceAuthorizationService resourceAuthorizationService;

    @MockBean
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @MockBean
    private QuartzJobInitializer quartzJobInitializer;

    @MockBean(name = "grpcUmsClient")
    private GrpcUmsClient grpcUmsClient;

    @MockBean(name = "umsClient")
    private UmsClient umsClient;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ClusterPertainRepository clusterPertainRepository;

    @Inject
    private ScalingActivityRepository scalingActivityRepository;

    private DistroXAutoScaleScalingActivityV1Endpoint distroXAutoScaleScalingActivityV1Endpoint;

    @BeforeEach
    public void setup() {
        distroXAutoScaleScalingActivityV1Endpoint = new AutoscaleUserCrnClientBuilder(String.format(SERVICE_ADDRESS, port))
                .build().withCrn(TEST_USER_CRN).distroXAutoScaleScalingActivityV1Endpoint();

        Cluster testCluster = new Cluster();
        testCluster.setStackCrn(TEST_CLUSTER_CRN);
        testCluster.setStackName(TEST_CLUSTER_NAME);
        testCluster.setCloudPlatform("AWS");

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        clusterPertain.setWorkspaceId(TEST_WORKSPACE_ID);
        clusterPertain.setUserId(TEST_USER_ID.toString());
        clusterPertain.setUserCrn(TEST_USER_CRN);

        UserManagementProto.User user = UserManagementProto.User.newBuilder()
                .setCrn(TEST_USER_CRN).setEmail("dummyuser@cloudera.com").setUserId(TEST_USER_ID.toString()).build();
        testCluster.setClusterPertain(
                clusterPertainRepository.findFirstByUserCrn(clusterPertain.getUserCrn())
                        .orElseGet(() -> clusterPertainRepository.save(clusterPertain)));
        clusterRepository.save(testCluster);

        ScalingActivity scalingActivity = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                Instant.now().minus(150, MINUTES).toEpochMilli(), TEST_OPERATION_ID);

        scalingActivityRepository.saveAll(List.of(scalingActivity));

        when(grpcUmsClient.getUserDetails(anyString())).thenReturn(user);
        when(grpcUmsClient.getResourceRoles(any())).thenReturn(Set.of(
                "crn:altus:iam:us-west-1:altus:resourceRole:Owner",
                "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin",
                "crn:altus:iam:us-west-1:altus:resourceRole:DatahubAdmin"));

        doNothing().when(grpcUmsClient).assignResourceRole(anyString(), anyString(), anyString());
        when(grpcUmsClient.checkAccountRight(anyString(), anyString())).thenReturn(Boolean.TRUE);
        doNothing().when(resourceAuthorizationService).authorize(eq("crn:cdp:iam:us-west-1:accid:cluster:mockuser@cloudera.com"), any(), any());
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("http://clusterproxy"));
    }

    @AfterEach
    void tearDown() {
        scalingActivityRepository.deleteAll();
        clusterRepository.deleteAll();
        clusterPertainRepository.deleteAll();
    }

    @Test
    public void testGetScalingActivitiesInGivenDurationByClusterNameWithNameAsNull() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivitiesInGivenDurationByClusterName(TEST_CLUSTER_NAME_NULL, 60, 0, 10).getContent());
    }

    @Test
    public void testGetScalingActivitiesInGivenDurationByClusterCrnWithCrnAsNull() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivitiesInGivenDurationByClusterCrn(TEST_CLUSTER_CRN_NULL, 60, 0, 10).getContent());
    }

    @Test
    public void testGetScalingActivityUsingOperationIdAndClusterCrn() {
        DistroXAutoscaleScalingActivityResponse distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterCrn(TEST_CLUSTER_CRN, TEST_OPERATION_ID);
        assertEquals(TEST_OPERATION_ID, distroXAutoscaleScalingActivityResponse.getOperationId());
    }

    @Test
    public void testGetScalingActivityUsingOperationIdAndClusterName() {
        DistroXAutoscaleScalingActivityResponse distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterName(TEST_CLUSTER_NAME, TEST_OPERATION_ID);
        assertEquals(TEST_OPERATION_ID, distroXAutoscaleScalingActivityResponse.getOperationId());
    }

    @Test
    public void testGetScalingActivityUsingOperationIdAndClusterCrnWithIdNotFound() {
        assertThrows(NotFoundException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterCrn(TEST_CLUSTER_CRN, TEST_OPERATION_ID_9));
    }

    @Test
    public void testGetScalingActivityUsingOperationIdAndClusterNameWithIdNotFound() {
        assertThrows(NotFoundException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterName(TEST_CLUSTER_NAME, TEST_OPERATION_ID_9));
    }

    @Test
    public void testGetScalingActivityUsingOperationIdandClusterCrnWithNullId() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterCrn(TEST_CLUSTER_CRN, TEST_OPERATION_ID_NULL));
    }

    @Test
    public void testGetScalingActivityUsingOperationIdAndClusterNameWithNullId() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivityUsingOperationIdAndClusterName(TEST_CLUSTER_CRN, TEST_OPERATION_ID_NULL));
    }

    @Test
    public void testGetFailedScalingActivitiesInGivenDurationByClusterNameWithNullName() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getFailedScalingActivitiesInGivenDurationByClusterName(TEST_CLUSTER_NAME_NULL, 60, 0, 10).getContent());
    }

    @Test
    public void testGetFailedScalingActivitiesInGivenDurationByClusterCrnWithNullCrn() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getFailedScalingActivitiesInGivenDurationByClusterCrn(TEST_CLUSTER_CRN_NULL, 60, 0, 10).getContent());
    }

    @Test
    public void testGetScalingActivitiesInTimeRangeByClusterNameWithNullName() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivitiesBetweenIntervalByClusterName(TEST_CLUSTER_NAME_NULL,
                        Instant.now().minus(60, MINUTES).toEpochMilli(),
                        Instant.now().minus(30, MINUTES).toEpochMilli(), 0, 10).getContent());
    }

    @Test
    public void testGetFailedScalingActivitiesInTimeRangeByClusterNameWithNullName() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getFailedScalingActivitiesBetweenIntervalByClusterName(TEST_CLUSTER_NAME_NULL,
                        Instant.now().minus(60, MINUTES).toEpochMilli(),
                        Instant.now().minus(30, MINUTES).toEpochMilli(), 0, 10).getContent());
    }

    @Test
    public void testGetScalingActivitiesInTimeRangeByClusterCrnWithNullCrn() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getScalingActivitiesBetweenIntervalByClusterCrn(TEST_CLUSTER_CRN_NULL,
                        Instant.now().minus(60, MINUTES).toEpochMilli(),
                        Instant.now().minus(30, MINUTES).toEpochMilli(), 0, 10).getContent());
    }

    @Test
    public void testGetFailedScalingActivitiesInTimeRangeByClusterCrnWithNullCrn() {
        assertThrows(IllegalStateException.class, () -> distroXAutoScaleScalingActivityV1Endpoint
                .getFailedScalingActivitiesBetweenIntervalByClusterCrn(TEST_CLUSTER_CRN_NULL,
                        Instant.now().minus(60, MINUTES).toEpochMilli(),
                        Instant.now().minus(30, MINUTES).toEpochMilli(), 0, 10).getContent());
    }

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, long creationTimestamp, String operationId) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setOperationId(operationId);
        scalingActivity.setFlowId(operationId);
        scalingActivity.setEndTime(new Date(Instant.now().toEpochMilli()));
        scalingActivity.setScalingActivityReason(TEST_REASON);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        scalingActivity.setCluster(cluster);
        return scalingActivity;
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        return cluster;
    }
}
