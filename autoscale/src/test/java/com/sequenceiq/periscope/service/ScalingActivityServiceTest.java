package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ActivityStatus.METRICS_COLLECTION_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_IN_PROGRESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.UPSCALE_TRIGGER_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;

@ExtendWith(MockitoExtension.class)
class ScalingActivityServiceTest {

    private static final String TEST_OPERATION_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final Long TEST_ACTIVITY_ID = 1L;

    private static final String TEST_ACTIVITY_REASON = "triggerReason";

    private static final String TEST_TENANT = "tenant";

    private static final String TEST_ACTIVITY_REASON_2 = "triggerReason2";

    private static final String TEST_FLOW_ID = "36d52d56-bcc7-434a-8468-1d2d66eba0a9";

    private static final String TEST_USER_CRN = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:91e155cb-ceac-4a76-94e1-de1ex3edb570";

    private static final String CLOUDBREAK_STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String CLOUDBREAK_STACK_NAME = "testCluster";

    @InjectMocks
    private ScalingActivityService underTest;

    @Mock
    private ScalingActivityRepository scalingActivityRepository;

    @Mock
    private RegionAwareCrnGenerator crnGenerator;

    @Mock
    private PeriscopeMetricService metricService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Captor
    private ArgumentCaptor<ScalingActivity> captor;

    @Test
    void testGetResourceCrnByResourceName() {
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(TEST_TENANT);
        when(clusterService.findOneByStackNameAndTenant(CLOUDBREAK_STACK_NAME, TEST_TENANT)).thenReturn(Optional.empty());
        AutoscaleStackV4Response autoscaleStackV4Response = mock(AutoscaleStackV4Response.class);
        when(cloudbreakCommunicator.getAutoscaleClusterByName(CLOUDBREAK_STACK_NAME, TEST_TENANT)).thenReturn(autoscaleStackV4Response);
        when(autoscaleStackV4Response.getStackType()).thenReturn(StackType.WORKLOAD);
        when(autoscaleStackV4Response.getClusterStatus()).thenReturn(Status.AVAILABLE);
        Cluster cluster = getCluster();
        when(clusterService.create(autoscaleStackV4Response)).thenReturn(cluster);

        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        assertEquals(CLOUDBREAK_STACK_CRN, ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () ->
                underTest.getResourceCrnByResourceName(CLOUDBREAK_STACK_NAME)));
    }

    @Test
    void testCreate() {
        Cluster cluster = getCluster();
        ActivityStatus status = METRICS_COLLECTION_SUCCESS;
        CrnTestUtil.mockCrnGenerator(crnGenerator);
        long now = Instant.now().toEpochMilli();
        underTest.create(cluster, status, TEST_ACTIVITY_REASON, now);

        verify(scalingActivityRepository, times(1)).save(captor.capture());
        ScalingActivity result = captor.getValue();

        assertThat(result).isInstanceOf(ScalingActivity.class);
        assertThat(result.getStartTime()).isEqualTo(new Date(now));
        assertThat(result.getActivityStatus()).isEqualTo(status);
    }

    @Test
    void testCreateWithYarnRecommendationFields() {
        Cluster cluster = getCluster();
        ActivityStatus status = METRICS_COLLECTION_SUCCESS;
        CrnTestUtil.mockCrnGenerator(crnGenerator);
        long now = Instant.now().toEpochMilli();
        underTest.create(cluster, status, TEST_ACTIVITY_REASON, now, now, TEST_ACTIVITY_REASON);

        verify(scalingActivityRepository, times(1)).save(captor.capture());
        ScalingActivity result = captor.getValue();

        assertThat(result).isInstanceOf(ScalingActivity.class);
        assertThat(result.getStartTime()).isEqualTo(new Date(now));
        assertThat(result.getActivityStatus()).isEqualTo(status);
    }

    @Test
    void testUpdate() {
        ActivityStatus newStatus = SCALING_FLOW_IN_PROGRESS;
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, TEST_FLOW_ID);
        ScalingActivity scalingActivity = createScalingActivity(TEST_ACTIVITY_ID, getCluster(), UPSCALE_TRIGGER_SUCCESS, TEST_ACTIVITY_REASON, null);
        doReturn(Optional.of(scalingActivity)).when(scalingActivityRepository).findById(anyLong());

        underTest.update(TEST_ACTIVITY_ID, flowIdentifier, newStatus, TEST_ACTIVITY_REASON_2);

        verify(scalingActivityRepository, times(1)).save(captor.capture());
        ScalingActivity result = captor.getValue();

        assertThat(result).isInstanceOf(ScalingActivity.class);
        assertThat(result.getActivityStatus()).isEqualTo(newStatus);
        assertThat(result.getFlowId()).isEqualTo(TEST_FLOW_ID);
        assertThat(result.getScalingActivityReason()).isEqualTo(TEST_ACTIVITY_REASON_2);
    }

    @Test
    void testFindByOperationIdAndClusterCrn() {
        ScalingActivity scalingActivity = createScalingActivity(TEST_ACTIVITY_ID, getCluster(), SCALING_FLOW_SUCCESS, TEST_ACTIVITY_REASON, TEST_FLOW_ID);
        doReturn(Optional.of(scalingActivity)).when(scalingActivityRepository).findByOperationIdAndClusterCrn(anyString(), anyString());

        ScalingActivity result = underTest.findByOperationIdAndClusterCrn(TEST_OPERATION_ID, CLOUDBREAK_STACK_CRN);

        verify(scalingActivityRepository).findByOperationIdAndClusterCrn(TEST_OPERATION_ID, CLOUDBREAK_STACK_CRN);
        assertThat(result).isNotNull();
    }

    @Test
    void testFindByOperationIdAndClusterName() {
        ScalingActivity scalingActivity = createScalingActivity(TEST_ACTIVITY_ID, getCluster(), SCALING_FLOW_SUCCESS, TEST_ACTIVITY_REASON, TEST_FLOW_ID);
        doReturn(Optional.of(scalingActivity)).when(scalingActivityRepository).findByOperationIdAndClusterName(anyString(), anyString());

        ScalingActivity result = underTest.findByOperationIdAndClusterName(TEST_OPERATION_ID, CLOUDBREAK_STACK_NAME);

        verify(scalingActivityRepository).findByOperationIdAndClusterName(TEST_OPERATION_ID, CLOUDBREAK_STACK_NAME);
        assertThat(result).isNotNull();
    }

    @Test
    void testNotFoundUsingOperationIdAndClusterCrn() {
        assertThatThrownBy(() -> underTest.findByOperationIdAndClusterCrn(TEST_OPERATION_ID, CLOUDBREAK_STACK_CRN)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testNotFoundUsingOperationIdAndClusterName() {
        assertThatThrownBy(() -> underTest.findByOperationIdAndClusterName(TEST_OPERATION_ID, CLOUDBREAK_STACK_NAME)).isInstanceOf(NotFoundException.class);
    }

    private Cluster getCluster() {
        Cluster cluster = new Cluster();
        cluster.setStackName(CLOUDBREAK_STACK_NAME);
        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setUserCrn(TEST_USER_CRN);

        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }

    private ScalingActivity createScalingActivity(Long id, Cluster cluster, ActivityStatus status, String reason, String flowId) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setId(id);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setCluster(cluster);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setFlowId(flowId);
        return scalingActivity;
    }
}
