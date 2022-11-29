package com.sequenceiq.periscope.repository;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.periscope.api.model.ActivityStatus;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.ScalingActivity;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories("com.sequenceiq.periscope.repository")
@EntityScan("com.sequenceiq.periscope.domain")
@DataJpaTest
class ScalingActivityRepositoryTest {

    private static final String TEST_ACTIVITY_CRN = "crn:cdp:autoscale:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:datahubAutoscaleActivity:6d" +
            "d833d4-7bc6-4a65-992a-b5da3400312c";

    private static final String TEST_REASON = "test trigger reason";

    private static final String CLOUDBREAK_STACK_CRN_1 = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String TEST_TENANT = "testTenant";

    @Inject
    private ScalingActivityRepository underTest;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private ClusterPertainRepository clusterPertainRepository;

    private ScalingActivity testScalingActivity;

    private Cluster testCluster;

    @BeforeEach
    void setUp() {
        testCluster = getACluster();
        testScalingActivity = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_SUCCESS, Instant.now().toEpochMilli());

        saveScalingActivity(testCluster, testScalingActivity);
    }

    @Test
    void testFindByTriggerCrn() {
        ScalingActivity result = underTest.findByActivityCrn(TEST_ACTIVITY_CRN).orElse(null);

        assertThat(result).isNotNull().isEqualTo(testScalingActivity);
    }

    @Test
    void testFindAllByCluster() {
        Cluster cluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(cluster, ActivityStatus.METRICS_COLLECTION_SUCCESS, Instant.now().toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS, Instant.now().toEpochMilli());

        saveScalingActivity(cluster, scalingActivity1);
        saveScalingActivity(testCluster, scalingActivity2);

        List<ScalingActivity> result = underTest.findAllByCluster(testCluster.getId());

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(testScalingActivity, scalingActivity2));
    }

    @Test
    void testFindAllByClusterWithStartTimeBefore() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.minus(5, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                now.minus(30, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3);

        List<ScalingActivity> result = underTest.findAllByClusterWithStartTimeBefore(testCluster.getId(),
                new Date(now.minus(10, MINUTES).toEpochMilli()));

        assertThat(result).hasSize(1).hasSameElementsAs(List.of(scalingActivity3));
    }

    @Test
    void testFindAllByClusterWithStartTimeAfter() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(5, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(30, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3);

        List<ScalingActivity> result = underTest.findAllByClusterWithStartTimeAfter(testCluster.getId(),
                new Date(now.minus(10, MINUTES).toEpochMilli()));

        assertThat(result).hasSize(3).hasSameElementsAs(Arrays.asList(testScalingActivity, scalingActivity1, scalingActivity2));
    }

    @Test
    void testFindAllByClusterAndTriggerStatusBetweenInterval() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(15, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        Date start = new Date(now.minus(15, MINUTES).toEpochMilli());
        Date end = new Date(now.plus(10, MINUTES).toEpochMilli());
        List<ScalingActivity> result = underTest.findAllByClusterAndActivityStatusBetweenInterval(testCluster.getId(),
                ActivityStatus.METRICS_COLLECTION_FAILED, start, end);

        assertThat(result).hasSize(1).hasSameElementsAs(List.of(scalingActivity2));
    }

    @Test
    void testFindAllByClusterInTriggerStatuses() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.plus(15, MINUTES).toEpochMilli());

        Set<ActivityStatus> statuses = Set.of(ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED);

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        List<ScalingActivity> result = underTest.findAllByClusterAndInStatuses(testCluster.getId(), statuses);

        assertThat(result).hasSize(3).hasSameElementsAs(Arrays.asList(scalingActivity2, scalingActivity3, scalingActivity4));
    }

    @Test
    void testFindAllByClusterBetweenInterval() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.plus(15, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        Date start = new Date(now.minus(10, MINUTES).toEpochMilli());
        Date end = new Date(now.plus(10, MINUTES).toEpochMilli());

        List<ScalingActivity> result = underTest.findAllByClusterBetweenInterval(testCluster.getId(), start, end);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(testScalingActivity, scalingActivity1));
    }

    @Test
    void testDeleteAllForCluster() {
        Instant now = Instant.now();
        Cluster cluster = getACluster();

        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());

        ScalingActivity scalingActivity3 = createScalingActivity(cluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(4, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(cluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(8, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2);
        saveScalingActivity(cluster, scalingActivity3, scalingActivity4);

        underTest.deleteAllByCluster(testCluster.getId());
        List<ScalingActivity> postDelete = underTest.findAllByCluster(testCluster.getId());
        List<ScalingActivity> undeleted = underTest.findAllByCluster(cluster.getId());

        assertThat(postDelete).isEmpty();
        assertThat(undeleted).hasSize(2).hasSameElementsAs(Arrays.asList(scalingActivity3, scalingActivity4));
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

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, long creationTimestamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setActivityCrn(TEST_ACTIVITY_CRN);
        scalingActivity.setScalingActivityReason(TEST_REASON);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setStartTime(new Date(creationTimestamp));

        scalingActivity.setCluster(cluster);

        return scalingActivity;
    }

    private void saveScalingActivity(Cluster cluster, ScalingActivity... scalingActivities) {
        clusterPertainRepository.save(cluster.getClusterPertain());
        clusterRepository.save(cluster);
        underTest.saveAll(Arrays.asList(scalingActivities));
    }

}