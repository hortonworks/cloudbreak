package com.sequenceiq.periscope.monitor.evaluator.load;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.UpdateFailedDetails;
import com.sequenceiq.periscope.model.adjustment.MandatoryScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.adjustment.RegularScalingAdjustmentParameters;
import com.sequenceiq.periscope.model.adjustment.StopStartScalingAdjustmentParameters;
import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EventPublisher;
import com.sequenceiq.periscope.monitor.event.UpdateFailedEvent;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.monitor.handler.CloudbreakCommunicator;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.RegularScalingAdjustmentService;
import com.sequenceiq.periscope.service.StopStartScalingAdjustmentService;
import com.sequenceiq.periscope.service.YarnBasedScalingAdjustmentService;
import com.sequenceiq.periscope.utils.MockStackResponseGenerator;
import com.sequenceiq.periscope.utils.StackResponseUtils;

@ExtendWith(MockitoExtension.class)
public class YarnLoadEvaluatorTest {

    private static final long AUTOSCALE_CLUSTER_ID = 101L;

    private static final String CLOUDBREAK_STACK_CRN = "someCrn";

    private static final String HOSTGROUP = "compute";

    private static final Integer HOSTGROUP_MIN_SIZE = 15;

    private static final Integer HOSTGROUP_MAX_SIZE = 75;

    private static final String FQDN_BASE = "test_fqdn";

    private static final String MACHINE_USER_CRN = "machineUserCrn";

    private static final String USER_CRN = "someUserCrn";

    @InjectMocks
    private YarnLoadEvaluator underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Mock
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Mock
    private StackResponseUtils stackResponseUtils;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private YarnBasedScalingAdjustmentService yarnBasedScalingAdjustmentService;

    @Mock
    private StopStartScalingAdjustmentService stopStartScalingAdjustmentService;

    @Mock
    private RegularScalingAdjustmentService regularScalingAdjustmentService;

    @Captor
    private ArgumentCaptor<MandatoryScalingAdjustmentParameters> adjustmentParamsCaptor;

    @Test
    void testRunCallsFinished() {
        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));
        doThrow(new RuntimeException("exception from the test")).when(clusterService).findById(anyLong());

        underTest.run();

        verify(executorServiceWithRegistry).finished(underTest, AUTOSCALE_CLUSTER_ID);
        verify(eventPublisher).publishEvent(any(UpdateFailedEvent.class));
    }

    @Test
    void testExecuteBeforeCoolDownPeriod() {
        Cluster cluster = getARunningCluster();
        cluster.setLastScalingActivity(Instant.now()
                .minus(2, ChronoUnit.MINUTES).toEpochMilli());
        doReturn(cluster).when(clusterService).findById(anyLong());
        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));

        underTest.execute();

        verifyNoInteractions(cloudbreakCommunicator, stopStartScalingAdjustmentService, yarnBasedScalingAdjustmentService);
    }

    @Test
    void testExecutePollingUserCrnFallsBackToUserCrnIfMachineUserNotInitialised() throws Exception {
        Cluster cluster = getARunningCluster();
        cluster.setMachineUserCrn(null);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                26, HOSTGROUP_MAX_SIZE - 26);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verifyNoInteractions(stopStartScalingAdjustmentService);
        verify(yarnBasedScalingAdjustmentService, times(1)).pollYarnMetricsAndScaleCluster(cluster, USER_CRN, Boolean.TRUE, stackResponse);
    }

    @Test
    void testExecutePollingUserCrnIsEqualToUserCrnIfLastExceptionIsLessThan1Hour() throws Exception {
        Cluster cluster = getARunningCluster();
        cluster.setMachineUserCrn(MACHINE_USER_CRN);
        UpdateFailedDetails updateFailedDetails = new UpdateFailedDetails(Instant.now().minus(59, ChronoUnit.MINUTES).toEpochMilli(),
                3L, Boolean.TRUE);
        cluster.setUpdateFailedDetails(updateFailedDetails);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                34, HOSTGROUP_MAX_SIZE - 34);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verifyNoInteractions(stopStartScalingAdjustmentService);
        verify(yarnBasedScalingAdjustmentService, times(1)).pollYarnMetricsAndScaleCluster(cluster, USER_CRN, Boolean.TRUE, stackResponse);
    }

    @Test
    void testExecutePollingUserCrnIsEqualToMachineUserCrnIfLastExceptionIsMoreThan1Hour() throws Exception {
        Cluster cluster = getARunningCluster();
        cluster.setMachineUserCrn(MACHINE_USER_CRN);
        UpdateFailedDetails updateFailedDetails = new UpdateFailedDetails(Instant.now().minus(61, ChronoUnit.MINUTES).toEpochMilli(),
                2L, Boolean.TRUE);
        cluster.setUpdateFailedDetails(updateFailedDetails);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                57, HOSTGROUP_MAX_SIZE - 57);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verifyNoInteractions(stopStartScalingAdjustmentService);
        verify(yarnBasedScalingAdjustmentService, times(1)).pollYarnMetricsAndScaleCluster(cluster, MACHINE_USER_CRN, Boolean.TRUE,
                stackResponse);
    }

    @Test
    void testExecuteWithMandatoryStopStartUpscaleAdjustment() {
        Cluster cluster = getARunningCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                37, 25);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verify(stopStartScalingAdjustmentService, times(1)).performMandatoryAdjustment(eq(cluster), eq(MACHINE_USER_CRN),
                eq(stackResponse), adjustmentParamsCaptor.capture());
        verifyNoInteractions(yarnBasedScalingAdjustmentService);

        MandatoryScalingAdjustmentParameters adjustmentParams = adjustmentParamsCaptor.getValue();
        assertThat(adjustmentParams.getUpscaleAdjustment()).isNotNull().isEqualTo(HOSTGROUP_MAX_SIZE - 37 - 25);
        assertThat(adjustmentParams.getDownscaleAdjustment()).isNull();
        assertThat(adjustmentParams).isInstanceOf(StopStartScalingAdjustmentParameters.class);
    }

    @Test
    void testExecuteWithMandatoryStopStartDownscaleAdjustment() {
        Cluster cluster = getARunningCluster();
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                45, 68);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verify(stopStartScalingAdjustmentService, times(1)).performMandatoryAdjustment(eq(cluster), eq(MACHINE_USER_CRN),
                eq(stackResponse), adjustmentParamsCaptor.capture());
        verifyNoInteractions(yarnBasedScalingAdjustmentService);

        MandatoryScalingAdjustmentParameters adjustmentParams = adjustmentParamsCaptor.getValue();
        assertThat(adjustmentParams.getUpscaleAdjustment()).isNull();
        assertThat(adjustmentParams.getDownscaleAdjustment()).isNotNull().isEqualTo(45 + 68 - HOSTGROUP_MAX_SIZE);
        assertThat(adjustmentParams).isInstanceOf(StopStartScalingAdjustmentParameters.class);
    }

    @Test
    void testExecuteWithMandatoryUpscaleAdjustment() {
        Cluster cluster = getARunningCluster();
        cluster.setStopStartScalingEnabled(Boolean.FALSE);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                10, 0);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verify(regularScalingAdjustmentService, times(1)).performMandatoryAdjustment(eq(cluster), eq(MACHINE_USER_CRN),
                eq(stackResponse), adjustmentParamsCaptor.capture());
        verifyNoInteractions(yarnBasedScalingAdjustmentService);

        MandatoryScalingAdjustmentParameters adjustmentParams = adjustmentParamsCaptor.getValue();
        assertThat(adjustmentParams.getUpscaleAdjustment()).isEqualTo(HOSTGROUP_MIN_SIZE - 10);
        assertThat(adjustmentParams.getDownscaleAdjustment()).isNull();
        assertThat(adjustmentParams).isInstanceOf(RegularScalingAdjustmentParameters.class);
    }

    @Test
    void testExecuteWithMandatoryDownscaleAdjustment() {
        Cluster cluster = getARunningCluster();
        cluster.setStopStartScalingEnabled(Boolean.FALSE);
        StackV4Response stackResponse = MockStackResponseGenerator.getMockStackV4Response(CLOUDBREAK_STACK_CRN, HOSTGROUP, FQDN_BASE,
                100, 0);

        setupBasicMocks(cluster, stackResponse);

        underTest.execute();

        verify(regularScalingAdjustmentService, times(1)).performMandatoryAdjustment(eq(cluster), eq(MACHINE_USER_CRN),
                eq(stackResponse), adjustmentParamsCaptor.capture());
        verifyNoInteractions(yarnBasedScalingAdjustmentService);

        MandatoryScalingAdjustmentParameters adjustmentParams = adjustmentParamsCaptor.getValue();
        assertThat(adjustmentParams.getDownscaleAdjustment()).isNotNull().isEqualTo(100 - HOSTGROUP_MAX_SIZE);
        assertThat(adjustmentParams.getUpscaleAdjustment()).isNull();
        assertThat(adjustmentParams).isInstanceOf(RegularScalingAdjustmentParameters.class);
    }

    @Test
    void testExecuteWhenNoMandatoryAdjustmentRequired() throws Exception {
        Cluster cluster = getARunningCluster();
        StackV4Response stackV4Response = MockStackResponseGenerator.getMockStackV4ResponseWithStoppedAndRunningNodes(CLOUDBREAK_STACK_CRN, HOSTGROUP,
                FQDN_BASE, 42, HOSTGROUP_MAX_SIZE - 42);

        setupBasicMocks(cluster, stackV4Response);

        underTest.execute();

        verifyNoInteractions(stopStartScalingAdjustmentService);
        verify(yarnBasedScalingAdjustmentService, times(1)).pollYarnMetricsAndScaleCluster(cluster, MACHINE_USER_CRN, Boolean.TRUE,
                stackV4Response);
    }

    private Cluster getARunningCluster() {
        Cluster cluster = new Cluster();
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setId(AUTOSCALE_CLUSTER_ID);
        cluster.setStackCrn(CLOUDBREAK_STACK_CRN);
        cluster.setState(ClusterState.RUNNING);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setUserCrn(USER_CRN);
        clusterPertain.setTenant("testtenant");
        cluster.setClusterPertain(clusterPertain);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.LOAD_BASED);
        scalingPolicy.setHostGroup(HOSTGROUP);

        LoadAlertConfiguration alertConfiguration = new LoadAlertConfiguration();
        alertConfiguration.setCoolDownMinutes(10);
        alertConfiguration.setMaxResourceValue(HOSTGROUP_MAX_SIZE);
        alertConfiguration.setMinResourceValue(HOSTGROUP_MIN_SIZE);

        LoadAlert loadAlert = new LoadAlert();
        loadAlert.setScalingPolicy(scalingPolicy);
        loadAlert.setLoadAlertConfiguration(alertConfiguration);

        cluster.setLoadAlerts(Set.of(loadAlert));
        cluster.setLastScalingActivity(Instant.now()
                .minus(45, ChronoUnit.MINUTES).toEpochMilli());
        cluster.setMachineUserCrn(MACHINE_USER_CRN);
        return cluster;
    }

    private void setupBasicMocks(Cluster cluster, StackV4Response stackResponse) {
        doReturn(cluster).when(clusterService).findById(anyLong());
        doReturn(stackResponse).when(cloudbreakCommunicator).getByCrn(CLOUDBREAK_STACK_CRN);
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsWithServicesHealthyForHostGroup(any(StackV4Response.class), anyString());
        doCallRealMethod().when(stackResponseUtils).getCloudInstanceIdsForHostGroup(any(StackV4Response.class), anyString());
        underTest.setContext(new ClusterIdEvaluatorContext(AUTOSCALE_CLUSTER_ID));
    }

}
