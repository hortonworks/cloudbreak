package com.sequenceiq.periscope.converter;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleScalingActivityResponse;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;

@ExtendWith(MockitoExtension.class)
class DistroXAutoscaleScalingActivityResponseConverterTest {

    private static final String TEST_OPERATION_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String TEST_REASON = "test trigger reason";

    private static final String TEST_YARN_RECOMMENDATION = "test yarn recommendation";

    private static final String CLOUDBREAK_STACK_CRN_1 = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String TEST_TENANT = "testTenant";

    @InjectMocks
    private DistroXAutoscaleScalingActivityResponseConverter underTest = new DistroXAutoscaleScalingActivityResponseConverter();

    @Test
    void testAllActivityStatusesNeedToBeMapped() {
        EnumSet<ActivityStatus> allStatuses = EnumSet.allOf(ActivityStatus.class);
        assertThat(allStatuses).hasSameElementsAs(((Map) ReflectionTestUtils.getField(underTest, "ACTIVITY_STATUS_MAP")).keySet());
    }

    @Test
    void testConvertScalingActivityToResponse() {
        Cluster testCluster = getACluster();
        ScalingActivity testScalingActivity = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_SUCCESS, Instant.now().toEpochMilli(),
                Instant.now().minus(30, MINUTES).toEpochMilli());
        DistroXAutoscaleScalingActivityResponse result = underTest.convert(testScalingActivity);
        assertThat(result.getYarnRecommendation()).isNotNull().isEqualTo(TEST_YARN_RECOMMENDATION);
        assertThat(result.getOperationId()).isNotNull().isEqualTo(TEST_OPERATION_ID);
    }

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, long creationTimestamp, long endTimeStamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setOperationId(TEST_OPERATION_ID);
        scalingActivity.setScalingActivityReason(TEST_REASON);
        scalingActivity.setYarnRecommendation(TEST_YARN_RECOMMENDATION);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setFlowId(TEST_OPERATION_ID);
        scalingActivity.setStartTime(new Date(creationTimestamp));
        scalingActivity.setYarnRecommendationTime(new Date(creationTimestamp));
        scalingActivity.setEndTime(new Date(endTimeStamp));
        scalingActivity.setCluster(cluster);

        return scalingActivity;
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN_1);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        return cluster;
    }
}
