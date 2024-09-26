package com.sequenceiq.periscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.model.ScalingAdjustmentType;

@ExtendWith(MockitoExtension.class)
public class UsageReportingServiceTest {

    @InjectMocks
    private UsageReportingService underTest;

    @Mock
    private UsageReporter usageReporter;

    @Test
    public void testReportLoadBasedAutoscalingTriggered() {
        LoadAlert alert = new LoadAlert();
        alert.setName("testAlert");

        LoadAlertConfiguration la = new LoadAlertConfiguration();
        la.setMaxResourceValue(100);
        la.setMinResourceValue(10);
        la.setCoolDownMinutes(5);
        alert.setLoadAlertConfiguration(la);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup("compute");
        alert.setScalingPolicy(scalingPolicy);

        Cluster cluster = mock(Cluster.class);
        ClusterPertain clusterPertain = mock(ClusterPertain.class);

        when(cluster.getClusterPertain()).thenReturn(clusterPertain);
        when(clusterPertain.getTenant()).thenReturn("testTenant");
        when(cluster.getStackCrn()).thenReturn("testStackCrn");
        when(cluster.getStackName()).thenReturn("testStackName");

        underTest.reportAutoscalingTriggered(10, 20, ScalingStatus.SUCCESS,
                "Scaling was triggered", alert, cluster, ScalingAdjustmentType.REGULAR);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleTriggered> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleTriggered.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleTriggered(captor.capture());
        UsageProto.CDPDatahubAutoscaleTriggered actual = captor.getValue();

        assertEquals("testTenant", actual.getAutoscaleTriggerDetails().getAccountId(), "Tenant should match");
        assertEquals("testStackCrn", actual.getAutoscaleTriggerDetails().getClusterCrn(), "Cluster crn should match");
        assertEquals("Scaling was triggered", actual.getAutoscaleTriggerDetails().getAutoscalingAction(), "Scaling Action should match");
        assertEquals(20, actual.getAutoscaleTriggerDetails().getOriginalHostGroupNodeCount(), "Original Node Count should match");
        assertEquals(30, actual.getAutoscaleTriggerDetails().getDesiredHostGroupNodeCount(), "Desired Node Count should match");

        assertEquals("LOAD_BASED", actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyType().name(),
                "Alert Policy Type should match");
        assertEquals("{parameters=LoadAlertConfiguration{minResourceValue=10, maxResourceValue=100, coolDownMinutes=5, scaleUpCoolDownMinutes=null," +
                        " scaleDownCoolDownMinutes=null, maxScaleDownStepSize=100, maxScaleUpStepSize=100}}",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyParameters(),
                "Alert Policy Parameters should match");
    }

    @Test
    public void testReportScheduledBasedAutoscalingTriggered() {
        TimeAlert alert = new TimeAlert();
        alert.setName("timeAlert");
        alert.setTimeZone("GMT");
        alert.setCron("0 1 0 1 0");

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup("compute");
        scalingPolicy.setScalingAdjustment(10);
        scalingPolicy.setAdjustmentType(AdjustmentType.NODE_COUNT);
        alert.setScalingPolicy(scalingPolicy);

        Cluster cluster = mock(Cluster.class);
        ClusterPertain clusterPertain = mock(ClusterPertain.class);

        when(cluster.getClusterPertain()).thenReturn(clusterPertain);
        when(clusterPertain.getTenant()).thenReturn("testTenant");
        when(cluster.getStackCrn()).thenReturn("testStackCrn");
        when(cluster.getStackName()).thenReturn("testStackName");

        underTest.reportAutoscalingTriggered(10, 20, ScalingStatus.SUCCESS,
                "Scaling was triggered", alert, cluster, ScalingAdjustmentType.REGULAR);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleTriggered> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleTriggered.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleTriggered(captor.capture());
        UsageProto.CDPDatahubAutoscaleTriggered actual = captor.getValue();

        assertEquals("testTenant", actual.getAutoscaleTriggerDetails().getAccountId(), "Tenant should match");
        assertEquals("testStackCrn", actual.getAutoscaleTriggerDetails().getClusterCrn(), "Cluster crn should match");
        assertEquals("Scaling was triggered", actual.getAutoscaleTriggerDetails().getAutoscalingAction(), "Scaling Action should match");
        assertEquals(20, actual.getAutoscaleTriggerDetails().getOriginalHostGroupNodeCount(), "Original Node Count should match");
        assertEquals(30, actual.getAutoscaleTriggerDetails().getDesiredHostGroupNodeCount(), "Desired Node Count should match");

        assertEquals("TIME_BASED", actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyType().name(),
                "Alert Policy Type should match");
        assertEquals("{cron=0 1 0 1 0, scalingTarget=10, adjustmentType=NODE_COUNT, timeZone=GMT}",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyParameters(),
                "Alert Policy Parameters should match");
    }

    @Test
    public void testReportAutoscalingConfigChanged() {
        TimeAlert alert = new TimeAlert();
        alert.setName("testAlert");
        alert.setCron("0 0 0 0");
        alert.setTimeZone("GMT");

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup("compute");
        alert.setScalingPolicy(scalingPolicy);

        TimeAlert alert1 = new TimeAlert();
        alert1.setName("testAlert");
        alert1.setCron("0 0 0 0");
        alert1.setTimeZone("GMT");
        alert1.setScalingPolicy(scalingPolicy);

        ClusterPertain clusterPertain = new ClusterPertain();
        clusterPertain.setTenant("testAccount");

        Cluster cluster = new Cluster();
        cluster.setTimeAlerts(Set.of(alert, alert1));
        cluster.setClusterPertain(clusterPertain);
        cluster.setStackName("testCluster");
        cluster.setStackCrn("testStackCrn");
        cluster.setAutoscalingEnabled(Boolean.TRUE);
        cluster.setStopStartScalingEnabled(Boolean.TRUE);

        underTest.reportAutoscalingConfigChanged("testUserCrn", cluster);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleConfigChanged> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleConfigChanged.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleConfigChanged(captor.capture());
        UsageProto.CDPDatahubAutoscaleConfigChanged actual = captor.getValue();

        assertEquals("testUserCrn", actual.getUserCrn(), "UserCrn should match");
        assertEquals("testCluster", actual.getClusterName(), "ClusterName should match");
        assertEquals("testStackCrn", actual.getClusterCrn(), "ClusterCrn should match");
        assertEquals("testAccount", actual.getAccountId(), "AccountId should match");
        assertEquals(2, actual.getAutoscalingPolicyDefinitionCount(), "Policy Count should match");
        assertEquals(Boolean.TRUE, actual.getAutoscalingEnabled(), "Autoscaling enabled should match");
        assertEquals(Boolean.TRUE, actual.getStopStartScalingEnabled(), "Stopstart scaling enabled should match");
    }
}
