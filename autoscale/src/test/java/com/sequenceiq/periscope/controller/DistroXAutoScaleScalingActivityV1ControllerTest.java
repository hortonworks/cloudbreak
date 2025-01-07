package com.sequenceiq.periscope.controller;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ApiActivityStatus;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.converter.DistroXAutoscaleScalingActivityResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.NameOrCrn;
import com.sequenceiq.periscope.service.ScalingActivityService;

@ExtendWith(MockitoExtension.class)
public class DistroXAutoScaleScalingActivityV1ControllerTest {

    private static final Map<ActivityStatus, ApiActivityStatus> ACTIVITY_STATUS_MAP = Map.ofEntries(
            entry(ActivityStatus.ACTIVITY_PENDING, ApiActivityStatus.ACTIVITY_PENDING),
            entry(ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ApiActivityStatus.DOWNSCALE_TRIGGER_FAILED),
            entry(ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS, ApiActivityStatus.DOWNSCALE_TRIGGER_SUCCESS),
            entry(ActivityStatus.METRICS_COLLECTION_FAILED, ApiActivityStatus.METRICS_COLLECTION_FAILED),
            entry(ActivityStatus.METRICS_COLLECTION_SUCCESS, ApiActivityStatus.METRICS_COLLECTION_SUCCESS),
            entry(ActivityStatus.SCALING_FLOW_FAILED, ApiActivityStatus.SCALING_FLOW_FAILED),
            entry(ActivityStatus.SCALING_FLOW_IN_PROGRESS, ApiActivityStatus.SCALING_FLOW_IN_PROGRESS),
            entry(ActivityStatus.SCALING_FLOW_SUCCESS, ApiActivityStatus.SCALING_FLOW_SUCCESS),
            entry(ActivityStatus.UPSCALE_TRIGGER_SUCCESS, ApiActivityStatus.UPSCALE_TRIGGER_SUCCESS),
            entry(ActivityStatus.UPSCALE_TRIGGER_FAILED, ApiActivityStatus.UPSCALE_TRIGGER_FAILED),
            entry(ActivityStatus.UNKNOWN, ApiActivityStatus.UNKNOWN),
            entry(ActivityStatus.MANDATORY_DOWNSCALE, ApiActivityStatus.POLICY_ADJUSTMENT),
            entry(ActivityStatus.MANDATORY_UPSCALE, ApiActivityStatus.POLICY_ADJUSTMENT),
            entry(ActivityStatus.SCHEDULE_BASED_UPSCALE, ApiActivityStatus.SCHEDULE_BASED_UPSCALE),
            entry(ActivityStatus.SCHEDULE_BASED_DOWNSCALE, ApiActivityStatus.SCHEDULE_BASED_DOWNSCALE)
    );

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_CLUSTER_NAME = "testCluster";

    private static final String TEST_TENANT = "testTenant";

    private static final String TEST_CLUSTER_CRN = String.format("crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_OPERATION_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String TEST_OPERATION_ID_2 = "9d74eee4-1cad-45d7-b645-7ccf9edbb73e";

    private static final String TEST_OPERATION_ID_3 = "9d74eee4-1cad-45d7-b645-7ccf9edbb73f";

    private static final String TEST_REASON = "test trigger reason";

    @InjectMocks
    private DistroXAutoScaleScalingActivityV1Controller distroXAutoScaleScalingActivityV1Controller;

    @Mock
    private ScalingActivityService scalingActivityService;

    @Mock
    private DistroXAutoscaleScalingActivityResponseConverter distroXAutoscaleScalingActivityResponseConverter;

    private ApiActivityStatus convert(ActivityStatus activityStatus) {
        return ACTIVITY_STATUS_MAP.get(activityStatus);
    }

    private DistroXAutoscaleScalingActivityResponse convert(ScalingActivity scalingActivity) {
        DistroXAutoscaleScalingActivityResponse json = new DistroXAutoscaleScalingActivityResponse();
        json.setStartTime(scalingActivity.getStartTime());
        json.setYarnRecommendationTime(scalingActivity.getYarnRecommendationTime());
        json.setEndTime(scalingActivity.getEndTime());
        json.setOperationId(scalingActivity.getOperationId());
        json.setActivityStatus(convert(scalingActivity.getActivityStatus()));
        json.setScalingActivityReason(scalingActivity.getScalingActivityReason());
        return json;
    }

    @Test
    public void testGetScalingActivitiesInGivenDurationByClusterName() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(50, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3))).when(scalingActivityService)
                .findAllInGivenDurationForCluster(NameOrCrn.ofName(TEST_CLUSTER_NAME), 60,
                        PageRequest.of(5, 1, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivitiesInGivenDurationByClusterName(TEST_CLUSTER_NAME, 60, 5, 1).getContent()
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3));
    }

    @Test
    public void testGetScalingActivitiesInGivenDurationByClusterCrn() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                Instant.now().minus(30, MINUTES).toEpochMilli(), TEST_OPERATION_ID);
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(20, MINUTES).toEpochMilli(), TEST_OPERATION_ID_2);

        doReturn(new PageImpl<>(List.of(scalingActivity1, scalingActivity2))).when(scalingActivityService)
                .findAllInGivenDurationForCluster(NameOrCrn.ofCrn(TEST_CLUSTER_CRN), 60,
                        PageRequest.of(1, 2, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity1)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity1);
        doReturn(convert(scalingActivity2)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity2);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivitiesInGivenDurationByClusterCrn(TEST_CLUSTER_CRN, 60, 1, 2)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID, TEST_OPERATION_ID_2));
    }

    @Test
    public void testGetScalingActivityUsingOperationId() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                Instant.now().minus(150, MINUTES).toEpochMilli(), TEST_OPERATION_ID);

        doReturn(scalingActivity).when(scalingActivityService).findByOperationIdAndClusterCrn(TEST_OPERATION_ID, TEST_CLUSTER_CRN);
        doReturn(convert(scalingActivity)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity);

        DistroXAutoscaleScalingActivityResponse distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivityUsingOperationIdAndClusterCrn(TEST_CLUSTER_CRN, TEST_OPERATION_ID);
        assertEquals(TEST_OPERATION_ID, distroXAutoscaleScalingActivityResponse.getOperationId());

        doReturn(scalingActivity).when(scalingActivityService).findByOperationIdAndClusterName(TEST_OPERATION_ID, TEST_CLUSTER_NAME);
        doReturn(convert(scalingActivity)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity);

        distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivityUsingOperationIdAndClusterName(TEST_CLUSTER_NAME, TEST_OPERATION_ID);
        assertEquals(TEST_OPERATION_ID, distroXAutoscaleScalingActivityResponse.getOperationId());
    }

    @Test
    public void testGetFailedScalingActivitiesInGivenDurationByClusterName() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(50, MINUTES).toEpochMilli(), TEST_OPERATION_ID);
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(40, MINUTES).toEpochMilli(), TEST_OPERATION_ID_2);
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(20, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3, scalingActivity2, scalingActivity1))).when(scalingActivityService)
                .findAllByFailedStatusesInGivenDuration(NameOrCrn.ofName(TEST_CLUSTER_NAME), 60,
                        PageRequest.of(0, 3, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);
        doReturn(convert(scalingActivity2)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity2);
        doReturn(convert(scalingActivity1)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity1);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getFailedScalingActivitiesInGivenDurationByClusterName(TEST_CLUSTER_NAME, 60, 0, 3)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3, TEST_OPERATION_ID_2, TEST_OPERATION_ID));
    }

    @Test
    public void testGetFailedScalingActivitiesInGivenDurationByClusterCrn() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(20, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3))).when(scalingActivityService)
                .findAllByFailedStatusesInGivenDuration(NameOrCrn.ofCrn(TEST_CLUSTER_CRN), 600,
                        PageRequest.of(2, 1, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getFailedScalingActivitiesInGivenDurationByClusterCrn(TEST_CLUSTER_CRN, 600, 2, 1)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3));
    }

    @Test
    public void testGetFailedScalingActivitiesInTimeRangeByClusterName() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(5, MINUTES).toEpochMilli(), TEST_OPERATION_ID);
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(4, MINUTES).toEpochMilli(), TEST_OPERATION_ID_2);
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(2, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3, scalingActivity2, scalingActivity1))).when(scalingActivityService)
                .findAllByFailedStatusesInTimeRangeForCluster(NameOrCrn.ofName(TEST_CLUSTER_NAME), 1676481427849L, 1676483227849L,
                        PageRequest.of(0, 3, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);
        doReturn(convert(scalingActivity2)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity2);
        doReturn(convert(scalingActivity1)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity1);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getFailedScalingActivitiesBetweenIntervalByClusterName(TEST_CLUSTER_NAME, 1676481427849L,
                        1676483227849L, 0, 3)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3, TEST_OPERATION_ID_2, TEST_OPERATION_ID));
    }

    @Test
    public void testGetFailedScalingActivitiesInTimeRangeByClusterCrn() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(3, MINUTES).toEpochMilli(), TEST_OPERATION_ID);
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                Instant.now().minus(2, MINUTES).toEpochMilli(), TEST_OPERATION_ID_2);
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(1, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3, scalingActivity2, scalingActivity1))).when(scalingActivityService)
                .findAllByFailedStatusesInTimeRangeForCluster(NameOrCrn.ofCrn(TEST_CLUSTER_CRN), 1676481427849L, 1676483227849L,
                        PageRequest.of(0, 3, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);
        doReturn(convert(scalingActivity2)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity2);
        doReturn(convert(scalingActivity1)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity1);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getFailedScalingActivitiesBetweenIntervalByClusterCrn(TEST_CLUSTER_CRN, 1676481427849L,
                        1676483227849L, 0, 3)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3, TEST_OPERATION_ID_2, TEST_OPERATION_ID));
    }

    @Test
    public void testGetScalingActivitiesInTimeRangeByClusterName() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(20, MINUTES).toEpochMilli(), TEST_OPERATION_ID_3);

        doReturn(new PageImpl<>(List.of(scalingActivity3))).when(scalingActivityService).findAllInTimeRangeForCluster(NameOrCrn.ofName(TEST_CLUSTER_NAME),
                1676481427849L, 1676483227849L,
                PageRequest.of(2, 1, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity3)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity3);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivitiesBetweenIntervalByClusterName(TEST_CLUSTER_NAME,
                        1676481427849L,
                        1676483227849L, 2, 1)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID_3));
    }

    @Test
    public void testGetScalingActivitiesInTimeRangeByClusterCrn() {
        Cluster testCluster = getACluster();
        ScalingActivity scalingActivity = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                Instant.now().minus(20, MINUTES).toEpochMilli(), TEST_OPERATION_ID);

        doReturn(new PageImpl<>(List.of(scalingActivity))).when(scalingActivityService).findAllInTimeRangeForCluster(NameOrCrn.ofCrn(TEST_CLUSTER_CRN),
                1676481427849L, 1676483227849L,
                PageRequest.of(0, 2, Sort.by("startTime").descending()));
        doReturn(convert(scalingActivity)).when(distroXAutoscaleScalingActivityResponseConverter).convert(scalingActivity);

        List<String> distroXAutoscaleScalingActivityResponse = distroXAutoScaleScalingActivityV1Controller
                .getScalingActivitiesBetweenIntervalByClusterCrn(TEST_CLUSTER_CRN,
                        1676481427849L,
                        1676483227849L, 0, 2)
                .stream().map(DistroXAutoscaleScalingActivityResponse::getOperationId).collect(Collectors.toList());
        assertEquals(distroXAutoscaleScalingActivityResponse, List.of(TEST_OPERATION_ID));
    }

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, long creationTimestamp, String operationId) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setOperationId(operationId);
        scalingActivity.setFlowId(operationId);
        scalingActivity.setEndTime(new Date(Instant.now().toEpochMilli()));
        scalingActivity.setScalingActivityReason(TEST_REASON);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        scalingActivity.setYarnRecommendationTime(new Date(creationTimestamp));
        scalingActivity.setCluster(cluster);
        return scalingActivity;
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_CLUSTER_CRN);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);
        cluster.setStackName(TEST_CLUSTER_NAME);
        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        return cluster;
    }
}
