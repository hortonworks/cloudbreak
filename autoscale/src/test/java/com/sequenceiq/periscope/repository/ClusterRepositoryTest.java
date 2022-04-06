package com.sequenceiq.periscope.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;

@ExtendWith(SpringExtension.class)
@EnableJpaRepositories(basePackages = "com.sequenceiq.periscope.repository")
@EntityScan(basePackages = "com.sequenceiq.periscope.domain")
@DataJpaTest
class ClusterRepositoryTest {

    private static final String CLOUDBREAK_STACK_CRN_1 = "crn:cdp:datahub:us-west-1:tenant:cluster:878605d9-f9e9-44c6-9da6-e4bce9570ef5";

    private static final String CLOUDBREAK_STACK_CRN_2 = "crn:cdp:datahub:us-west-1:tenant:cluster:35ae1d9d-4c4b-4d11-a714-91eadcc5be8a";

    private static final String TEST_ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:c11d716f-8b87-432b-98ef-14a46399ea74";

    private static final int TEST_HOSTGROUP_MIN_SIZE = 3;

    private static final int TEST_HOSTGROUP_MAX_SIZE = 200;

    private static final String TEST_CRON_EXPRESSION = "0 0 0 * * ?";

    private static final String TEST_TENANT = "testtenant";

    @Inject
    private ClusterRepository underTest;

    @Inject
    private LoadAlertRepository loadAlertRepository;

    @Inject
    private TimeAlertRepository timeAlertRepository;

    @Inject
    private ScalingPolicyRepository scalingPolicyRepository;

    @Inject
    private ClusterPertainRepository clusterPertainRepository;

    private Cluster loadCluster;

    private Cluster timeCluster;

    @BeforeEach
    void setUp() {
        loadCluster = getAClusterWithLoadAlerts();
        saveWithLoadAlerts(loadCluster);

        timeCluster = getAClusterWithTimeAlerts();
        saveWithTimeAlerts(timeCluster);
    }

    @Test
    void testSave() {
        Cluster cluster = getAClusterWithLoadAlerts();

        assertThat(underTest.save(cluster)).isNotNull();
    }

    @Test
    void testFindStackCrnById() {
        String result = underTest.findStackCrnById(loadCluster.getId());

        assertThat(result).isEqualTo(CLOUDBREAK_STACK_CRN_1);
    }

    @Test
    void testFindClustersByClusterIds() {
        List<Cluster> result = underTest.findClustersByClusterIds(Arrays.asList(loadCluster.getId(), timeCluster.getId()));

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadCluster, timeCluster));
    }

    @Test
    void testFindByTenantAndStackType() {
        List<Cluster> result = underTest.findByTenantAndStackType(TEST_TENANT, StackType.WORKLOAD);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadCluster, timeCluster));
    }

    @Test
    void testFindByEnvironmentCrnOrMachineUserCrnWithAllNull() {
        List<Cluster> result = underTest.findByEnvironmentCrnOrMachineUserCrn(null, null);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadCluster, timeCluster));
    }

    @Test
    void testFindByEnvironmentCrnOrMachineUserCrnWithEnvironmentCrn() {
        Cluster cluster = getAClusterWithLoadAlerts();
        cluster.setEnvironmentCrn(TEST_ENV_CRN);
        saveWithLoadAlerts(cluster);

        List<Cluster> result = underTest.findByEnvironmentCrnOrMachineUserCrn(TEST_ENV_CRN, null);

        assertThat(result).hasSize(3).hasSameElementsAs(Arrays.asList(loadCluster, timeCluster, cluster));
    }

    @Test
    void testFindIdStackCrn() {
        Long result = underTest.findIdStackCrn(CLOUDBREAK_STACK_CRN_1);

        assertThat(result).isEqualTo(loadCluster.getId());
    }

    @Test
    void testFindByStackCrnAndTenant() {
        Optional<Cluster> result = underTest.findByStackCrnAndTenant(CLOUDBREAK_STACK_CRN_1, TEST_TENANT);

        assertThat(result).contains(loadCluster);
    }

    @Test
    void testFindByLoadAlertAndStackTypeAndClusterStateAndAutoscaling() {
        List<Long> result = underTest.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(StackType.WORKLOAD,
                ClusterState.RUNNING, Boolean.TRUE, null);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(loadCluster.getId());
    }

    @Test
    void testFindByLoadAlertAndStackTypeAndClusterStateAndAutoscalingWithWrongStackType() {
        List<Long> result = underTest.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(StackType.DATALAKE,
                ClusterState.RUNNING, Boolean.TRUE, null);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindByLoadAlertAndStackTypeAndClusterStateAndAutoscalingWithDifferentClusterState() {
        Cluster suspendedCluster = getAClusterWithLoadAlerts();
        suspendedCluster.setState(ClusterState.SUSPENDED);
        saveWithLoadAlerts(suspendedCluster);

        List<Long> result = underTest.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(StackType.WORKLOAD,
                ClusterState.SUSPENDED, Boolean.TRUE, null);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(suspendedCluster.getId());
    }

    @Test
    void testFindByLoadAlertAndStackTypeAndClusterStateAndAutoscalingNotEnabled() {
        Cluster loadClusterWithAutoscalingDisabled = getAClusterWithLoadAlerts();
        loadClusterWithAutoscalingDisabled.setAutoscalingEnabled(Boolean.FALSE);
        saveWithLoadAlerts(loadClusterWithAutoscalingDisabled);

        List<Long> result = underTest.findByLoadAlertAndStackTypeAndClusterStateAndAutoscaling(StackType.WORKLOAD,
                ClusterState.RUNNING, Boolean.FALSE, null);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(loadClusterWithAutoscalingDisabled.getId());
    }

    @Test
    void testFindByTimeAlertAndStackTypeAndAutoscaling() {
        List<Long> result = underTest.findByTimeAlertAndStackTypeAndAutoscaling(StackType.WORKLOAD,
                Boolean.TRUE, null);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(timeCluster.getId());
    }

    @Test
    void testFindByTimeAlertAndStackTypeAndAutoscalingNotEnabled() {
        Cluster timeClusterWithAutoscalingDisabled = getAClusterWithTimeAlerts();
        timeClusterWithAutoscalingDisabled.setAutoscalingEnabled(Boolean.FALSE);
        saveWithTimeAlerts(timeClusterWithAutoscalingDisabled);

        List<Long> result = underTest.findByTimeAlertAndStackTypeAndAutoscaling(StackType.WORKLOAD,
                Boolean.FALSE, null);

        assertThat(result).hasSize(1);
        assertThat(result.iterator().next()).isEqualTo(timeClusterWithAutoscalingDisabled.getId());
    }

    @Test
    void testFindClusterIdsByStackTypeAndPeriscopeNodeId() {
        List<Long> result = underTest.findClusterIdsByStackTypeAndPeriscopeNodeId(StackType.WORKLOAD,
                null);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadCluster.getId(), timeCluster.getId()));
    }

    @Test
    void testFindClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled() {
        List<Long> result = underTest.findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(StackType.WORKLOAD,
                null, Boolean.TRUE);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadCluster.getId(), timeCluster.getId()));
    }

    @Test
    void testFindClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingDisabled() {
        Cluster loadClusterWithAutoscalingDisabled = getAClusterWithLoadAlerts();
        loadClusterWithAutoscalingDisabled.setAutoscalingEnabled(Boolean.FALSE);
        saveWithLoadAlerts(loadClusterWithAutoscalingDisabled);

        Cluster timeClusterWithAutoscalingDisabled = getAClusterWithTimeAlerts();
        timeClusterWithAutoscalingDisabled.setAutoscalingEnabled(Boolean.FALSE);
        saveWithTimeAlerts(timeClusterWithAutoscalingDisabled);

        List<Long> result = underTest.findClusterIdsByStackTypeAndPeriscopeNodeIdAndAutoscalingEnabled(StackType.WORKLOAD,
                null, Boolean.FALSE);

        assertThat(result).hasSize(2).hasSameElementsAs(Arrays.asList(loadClusterWithAutoscalingDisabled.getId(), timeClusterWithAutoscalingDisabled.getId()));
    }

    private Cluster getAClusterWithLoadAlerts() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN_1);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicy.setHostGroup("compute");

        LoadAlertConfiguration alertConfiguration = new LoadAlertConfiguration();
        alertConfiguration.setCoolDownMinutes(10);
        alertConfiguration.setMaxResourceValue(TEST_HOSTGROUP_MAX_SIZE);
        alertConfiguration.setMinResourceValue(TEST_HOSTGROUP_MIN_SIZE);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setScalingPolicy(scalingPolicy);
        loadAlert.setLoadAlertConfiguration(alertConfiguration);
        loadAlert.setCluster(cluster);

        cluster.setLoadAlerts(Set.of(loadAlert));
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());
        return cluster;
    }

    private Cluster getAClusterWithTimeAlerts() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN_2);
        cluster.setState(ClusterState.RUNNING);
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStackType(StackType.WORKLOAD);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant(TEST_TENANT);
        cluster.setClusterPertain(clusterPertain);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.EXACT);
        scalingPolicy.setHostGroup("compute");

        TimeAlert alert = new TimeAlert();
        alert.setCron(TEST_CRON_EXPRESSION);
        alert.setCluster(cluster);
        alert.setScalingPolicy(scalingPolicy);

        cluster.setTimeAlerts(Set.of(alert));
        return cluster;
    }

    private void saveWithLoadAlerts(Cluster cluster) {
        underTest.save(cluster);
        loadAlertRepository.save(cluster.getLoadAlerts().iterator().next());
        scalingPolicyRepository.save(cluster.getLoadAlerts().iterator().next().getScalingPolicy());
        clusterPertainRepository.save(cluster.getClusterPertain());
    }

    private void saveWithTimeAlerts(Cluster cluster) {
        underTest.save(cluster);
        timeAlertRepository.save(cluster.getTimeAlerts().iterator().next());
        scalingPolicyRepository.save(cluster.getTimeAlerts().iterator().next().getScalingPolicy());
        clusterPertainRepository.save(cluster.getClusterPertain());
    }
}