package com.sequenceiq.periscope.repository;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final String TEST_OPERATION_ID = "9d74eee4-1cad-45d7-b645-7ccf9edbb73d";

    private static final String TEST_REASON = "test trigger reason";

    private static final String CLOUDBREAK_STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String CLOUDBREAK_STACK_NAME = "testCluster2";

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
    void testFindByOperationIdAndClusterCrn() {
        ScalingActivity result = underTest.findByOperationIdAndClusterCrn(TEST_OPERATION_ID, CLOUDBREAK_STACK_CRN).orElse(null);

        assertThat(result).isNotNull().isEqualTo(testScalingActivity);
    }

    @Test
    void testFindByOperationIdAndClusterName() {
        ScalingActivity result = underTest.findByOperationIdAndClusterName(TEST_OPERATION_ID, CLOUDBREAK_STACK_NAME).orElse(null);

        assertThat(result).isNotNull().isEqualTo(testScalingActivity);
    }

    @Test
    void testFindAllByCluster() {
        Cluster cluster = getACluster();
        ScalingActivity scalingActivity1 = createScalingActivity(cluster, ActivityStatus.METRICS_COLLECTION_SUCCESS, Instant.now().toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS, Instant.now().toEpochMilli());

        testCluster.setStackName("testCluster");
        saveScalingActivity(cluster, scalingActivity1);
        saveScalingActivity(testCluster, scalingActivity2);

        List<ScalingActivity> result = underTest.findAllByCluster(testCluster.getStackName());

        assertThat(result).hasSize(2).hasSameElementsAs(asList(testScalingActivity, scalingActivity2));
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
    void testFindAllByClusterWithStartTimeAfterUsingClusterCrn() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(5, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(30, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ScalingActivity> result = underTest.findAllByClusterCrnWithStartTimeAfter(testCluster.getStackCrn(),
                new Date(now.minus(10, MINUTES).toEpochMilli()), pageable);

        assertThat(result.getContent()).hasSize(3).hasSameElementsAs(asList(testScalingActivity, scalingActivity1, scalingActivity2));
    }

    @Test
    void testFindAllByClusterWithStartTimeAfterUsingClusterName() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(5, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(30, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3);

        Pageable pageable = PageRequest.of(0, 1);
        Page<ScalingActivity> result = underTest.findAllByClusterNameWithStartTimeAfter(testCluster.getStackName(),
                new Date(now.minus(10, MINUTES).toEpochMilli()), pageable);

        assertThat(result.getContent()).hasSize(1).hasSameElementsAs(asList(testScalingActivity));
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

        assertThat(result).hasSize(3).hasSameElementsAs(asList(scalingActivity2, scalingActivity3, scalingActivity4));
    }

    @Test
    void testfindAllByClusterAndInStatusesAndInGivenIntervalUsingClusterCrn() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.plus(25, MINUTES).toEpochMilli());

        Set<ActivityStatus> statuses = Set.of(ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED);

        Date start = new Date(now.minus(30, MINUTES).toEpochMilli());
        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ScalingActivity> result = underTest.findAllByClusterCrnAndInStatusesWithTimeAfter(testCluster.getStackCrn(),
                statuses, start, pageable);

        assertThat(result.getContent()).hasSize(3).hasSameElementsAs(asList(scalingActivity2, scalingActivity3, scalingActivity4));
    }

    @Test
    void testfindAllByClusterAndInStatusesAndInGivenIntervalUsingClusterName() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_SUCCESS,
                now.plus(15, MINUTES).toEpochMilli());

        Set<ActivityStatus> statuses = Set.of(ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                ActivityStatus.DOWNSCALE_TRIGGER_FAILED, ActivityStatus.SCALING_FLOW_FAILED);

        Date start = new Date(now.minus(30, MINUTES).toEpochMilli());
        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        Pageable pageable = PageRequest.of(0, 10);
        Page<ScalingActivity> result = underTest.findAllByClusterNameAndInStatusesWithTimeAfter(testCluster.getStackName(),
                statuses, start, pageable);

        assertThat(result.getContent()).hasSize(0).hasSameElementsAs(List.of());
    }

    @Test
    void testFindAllByClusterBetweenIntervalUsingClusterCrn() {
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
        Pageable pageable = PageRequest.of(0, 10);

        Page<ScalingActivity> result = underTest.findAllByClusterCrnBetweenInterval(testCluster.getStackCrn(), start, end, pageable);

        assertThat(result.getContent()).hasSize(2).hasSameElementsAs(asList(testScalingActivity, scalingActivity1));
    }

    @Test
    void testFindAllByClusterBetweenIntervalUsingClusterName() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.plus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.plus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity5 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_IN_PROGRESS,
                now.minus(5, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);

        Date start = new Date(now.minus(10, MINUTES).toEpochMilli());
        Date end = new Date(now.plus(10, MINUTES).toEpochMilli());
        Pageable pageable = PageRequest.of(1, 1);

        Page<ScalingActivity> result = underTest.findAllByClusterNameBetweenInterval(testCluster.getStackName(), start, end, pageable);

        assertThat(result.getContent()).hasSize(1).hasSameElementsAs(asList(scalingActivity1));
    }

    @Test
    void testfindAllByClusterNameAndInStatusesBetweenInterval() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                now.minus(11, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(1, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity5 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_IN_PROGRESS,
                now.minus(5, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4, scalingActivity5);

        Set<ActivityStatus> statuses = Set.of(ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                ActivityStatus.DOWNSCALE_TRIGGER_FAILED);
        Date start = new Date(now.minus(100, MINUTES).toEpochMilli());
        Date end = new Date(now.toEpochMilli());
        Pageable pageable = PageRequest.of(0, 5);

        Page<ScalingActivity> result = underTest.findAllByClusterNameAndInStatusesBetweenInterval(testCluster.getStackName(), statuses, start, end, pageable);

        assertThat(result.getContent()).hasSize(3).hasSameElementsAs(asList(scalingActivity2, scalingActivity3, scalingActivity4));
    }

    @Test
    void testfindAllByClusterCrnAndInStatusesBetweenInterval() {
        Instant now = Instant.now();
        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_FAILED,
                now.minus(15, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.DOWNSCALE_TRIGGER_FAILED,
                now.minus(11, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(1, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity5 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_IN_PROGRESS,
                now.minus(5, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4, scalingActivity5);

        Set<ActivityStatus> statuses = Set.of(ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                ActivityStatus.DOWNSCALE_TRIGGER_FAILED);
        Date start = new Date(now.minus(10, MINUTES).toEpochMilli());
        Date end = new Date(now.toEpochMilli());
        Pageable pageable = PageRequest.of(0, 5);

        Page<ScalingActivity> result = underTest.findAllByClusterCrnAndInStatusesBetweenInterval(testCluster.getStackCrn(), statuses, start, end, pageable);

        assertThat(result.getContent()).hasSize(1).hasSameElementsAs(asList(scalingActivity4));
    }

    @Test
    void testDeleteAllForCluster() {
        Instant now = Instant.now();
        Cluster cluster = getACluster();
        testCluster.setStackName("testCluster");

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
        List<ScalingActivity> postDelete = underTest.findAllByCluster(testCluster.getStackName());
        List<ScalingActivity> undeleted = underTest.findAllByCluster(cluster.getStackName());


        assertThat(postDelete).isEmpty();
        assertThat(undeleted).hasSize(2).hasSameElementsAs(asList(scalingActivity3, scalingActivity4));
    }

    @Test
    void testFindAllThatEndedBefore() {
        Instant now = Instant.now();

        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.UPSCALE_TRIGGER_FAILED,
                now.minus(8, MINUTES).toEpochMilli());
        scalingActivity1.setEndTime(new Date(now.minus(5, MINUTES).toEpochMilli()));

        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.minus(4, MINUTES).toEpochMilli());
        scalingActivity2.setEndTime(new Date(now.minus(2, MINUTES).toEpochMilli()));

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2);

        List<Long> result = underTest.findAllIdsWithEndTimeBefore(new Date(now.minus(5, MINUTES).toEpochMilli()));

        assertThat(result).hasSize(1).hasSameElementsAs(List.of(scalingActivity1.getId()));
    }

    @Test
    void testFindAllIdsInStatusesThatStartedBefore() {
        Instant now = Instant.now();

        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_FAILED,
                now.minus(20, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_SUCCESS,
                now.minus(30, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_SUCCESS,
                now.minus(5, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);
        Pageable pageable = PageRequest.of(0, 10);

        List<Long> result = underTest.findAllIdsInActivityStatusesWithStartTimeBefore(EnumSet.of(ActivityStatus.SCALING_FLOW_FAILED,
                ActivityStatus.SCALING_FLOW_SUCCESS), new Date(now.minus(8, MINUTES).toEpochMilli()), pageable);

        assertThat(result).hasSize(2).hasSameElementsAs(asList(scalingActivity1.getId(), scalingActivity2.getId()));
    }

    @Test
    void testFindAllIdsInStatusesThatStartedBeforeWithLimit2() {
        Instant now = Instant.now();

        ScalingActivity scalingActivity1 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_FAILED,
                now.minus(20, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity2 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_SUCCESS,
                now.minus(30, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity3 = createScalingActivity(testCluster, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                now.minus(10, MINUTES).toEpochMilli());
        ScalingActivity scalingActivity4 = createScalingActivity(testCluster, ActivityStatus.SCALING_FLOW_SUCCESS,
                now.minus(5, MINUTES).toEpochMilli());

        saveScalingActivity(testCluster, scalingActivity1, scalingActivity2, scalingActivity3, scalingActivity4);
        Pageable pageable = PageRequest.of(0, 1);

        List<Long> result = underTest.findAllIdsInActivityStatusesWithStartTimeBefore(EnumSet.of(ActivityStatus.SCALING_FLOW_FAILED,
                ActivityStatus.SCALING_FLOW_SUCCESS), new Date(now.minus(8, MINUTES).toEpochMilli()), pageable);

        assertThat(result).hasSize(1);
    }

    @Test
    void testCountAllInActivityStatuses() {
        Instant now = Instant.now();

        Set<ActivityStatus> statuses = EnumSet.of(ActivityStatus.UPSCALE_TRIGGER_SUCCESS, ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS,
                ActivityStatus.METRICS_COLLECTION_FAILED, ActivityStatus.MANDATORY_UPSCALE);
        List<ScalingActivity> activities = newArrayList();

        statuses.forEach(status -> {
            ScalingActivity activity = createScalingActivity(testCluster, status, now.minus(15, MINUTES).toEpochMilli());
            activities.add(activity);
        });

        saveScalingActivity(testCluster, activities);

        Long result = underTest.countAllInActivityStatuses(Set.of(ActivityStatus.MANDATORY_UPSCALE, ActivityStatus.METRICS_COLLECTION_SUCCESS,
                ActivityStatus.DOWNSCALE_TRIGGER_SUCCESS));

        assertThat(result).isEqualTo(2);
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);
        cluster.setStackName("testCluster2");

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        return cluster;
    }

    private ScalingActivity createScalingActivity(Cluster cluster, ActivityStatus status, long creationTimestamp) {
        ScalingActivity scalingActivity = new ScalingActivity();
        scalingActivity.setOperationId(TEST_OPERATION_ID);
        scalingActivity.setScalingActivityReason(TEST_REASON);
        scalingActivity.setActivityStatus(status);
        scalingActivity.setStartTime(new Date(creationTimestamp));

        scalingActivity.setCluster(cluster);

        return scalingActivity;
    }

    private void saveScalingActivity(Cluster cluster, ScalingActivity... scalingActivities) {
        clusterPertainRepository.save(cluster.getClusterPertain());
        clusterRepository.save(cluster);
        underTest.saveAll(asList(scalingActivities));
    }

    private void saveScalingActivity(Cluster cluster, List<ScalingActivity> activities) {
        clusterPertainRepository.save(cluster.getClusterPertain());
        clusterRepository.save(cluster);
        underTest.saveAll(activities);
    }

}
