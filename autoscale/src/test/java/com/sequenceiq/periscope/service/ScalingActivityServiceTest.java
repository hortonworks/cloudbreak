package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.ActivityStatus.METRICS_COLLECTION_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_IN_PROGRESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.UPSCALE_TRIGGER_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.repository.ScalingActivityRepository;

@ExtendWith(MockitoExtension.class)
class ScalingActivityServiceTest {

    private static final String TEST_ACTIVITY_CRN = "crn:cdp:autoscale:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:" +
            "datahubAutoscaleActivity:6dd833d4-7bc6-4a65-992a-b5da3400312c";

    private static final String TEST_ACTIVITY_REASON = "triggerReason";

    private static final String TEST_FLOW_ID = "36d52d56-bcc7-434a-8468-1d2d66eba0a9";

    private static final String TEST_USER_CRN = "crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:91e155cb-ceac-4a76-94e1-de1ex3edb570";

    @InjectMocks
    private ScalingActivityService underTest;

    @Mock
    private ScalingActivityRepository scalingActivityRepository;

    @Mock
    private RegionAwareCrnGenerator crnGenerator;

    @Captor
    private ArgumentCaptor<ScalingActivity> captor;

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
    void testUpdateWithFlowDetails() {
        ActivityStatus newStatus = SCALING_FLOW_IN_PROGRESS;
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, TEST_FLOW_ID);
        ScalingActivity scalingActivity = createScalingActivity(getCluster(), UPSCALE_TRIGGER_SUCCESS, TEST_ACTIVITY_REASON, null);
        doReturn(Optional.of(scalingActivity)).when(scalingActivityRepository).findByActivityCrn(anyString());

        underTest.updateWithFlowIdAndTriggerStatus(flowIdentifier, TEST_ACTIVITY_CRN, newStatus);

        verify(scalingActivityRepository, times(1)).save(captor.capture());
        ScalingActivity result = captor.getValue();

        assertThat(result).isInstanceOf(ScalingActivity.class);
        assertThat(result.getActivityStatus()).isEqualTo(newStatus);
        assertThat(result.getFlowId()).isEqualTo(TEST_FLOW_ID);
    }

    @Test
    void testFindByTriggerCrn() {
        ScalingActivity scalingActivity = createScalingActivity(getCluster(), SCALING_FLOW_SUCCESS, TEST_ACTIVITY_REASON, TEST_FLOW_ID);
        doReturn(Optional.of(scalingActivity)).when(scalingActivityRepository).findByActivityCrn(anyString());

        ScalingActivity result = underTest.findByCrn(TEST_ACTIVITY_CRN);

        verify(scalingActivityRepository).findByActivityCrn(TEST_ACTIVITY_CRN);
        assertThat(result).isNotNull();
    }

    @Test
    void testNotFoundByTriggerCrn() {
        assertThatThrownBy(() -> underTest.findByCrn(TEST_ACTIVITY_CRN)).isInstanceOf(NotFoundException.class);
    }

    private Cluster getCluster() {
        Cluster cluster =  new Cluster();

        ClusterPertain clusterPertain =  new ClusterPertain();
        clusterPertain.setUserCrn(TEST_USER_CRN);

        cluster.setClusterPertain(clusterPertain);
        return cluster;
    }

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, String reason, String flowId) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setActivityStatus(status);
        scalingActivity.setCluster(cluster);
        scalingActivity.setScalingActivityReason(reason);
        scalingActivity.setFlowId(flowId);
        return scalingActivity;
    }
}