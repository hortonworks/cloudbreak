package com.sequenceiq.periscope.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
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
                "Scaling was triggered", alert, cluster);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleTriggered> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleTriggered.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleTriggered(captor.capture());
        UsageProto.CDPDatahubAutoscaleTriggered actual = captor.getValue();

        assertEquals("Tenant should match", "testTenant", actual.getAutoscaleTriggerDetails().getAccountId());
        assertEquals("Cluster crn should match", "testStackCrn", actual.getAutoscaleTriggerDetails().getClusterCrn());
        assertEquals("Scaling Action should match", "Scaling was triggered", actual.getAutoscaleTriggerDetails().getAutoscalingAction());
        assertEquals("Original Node Count should match", 20, actual.getAutoscaleTriggerDetails().getOriginalHostGroupNodeCount());
        assertEquals("Desired Node Count should match", 30, actual.getAutoscaleTriggerDetails().getDesiredHostGroupNodeCount());

        assertEquals("Alert Policy Type should match", "LOAD_BASED",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyType().name());
        assertEquals("Alert Policy Parameters should match",
                "{parameters=LoadAlertConfiguration{minResourceValue=10, maxResourceValue=100, coolDownMinutes=5, scaleUpCoolDownMinutes=null," +
                        " scaleDownCoolDownMinutes=null, maxScaleDownStepSize=100, maxScaleUpStepSize=100}}",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyParameters());
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
                "Scaling was triggered", alert, cluster);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleTriggered> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleTriggered.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleTriggered(captor.capture());
        UsageProto.CDPDatahubAutoscaleTriggered actual = captor.getValue();

        assertEquals("Tenant should match", "testTenant", actual.getAutoscaleTriggerDetails().getAccountId());
        assertEquals("Cluster crn should match", "testStackCrn", actual.getAutoscaleTriggerDetails().getClusterCrn());
        assertEquals("Scaling Action should match", "Scaling was triggered", actual.getAutoscaleTriggerDetails().getAutoscalingAction());
        assertEquals("Original Node Count should match", 20, actual.getAutoscaleTriggerDetails().getOriginalHostGroupNodeCount());
        assertEquals("Desired Node Count should match", 30, actual.getAutoscaleTriggerDetails().getDesiredHostGroupNodeCount());

        assertEquals("Alert Policy Type should match", "TIME_BASED",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyType().name());
        assertEquals("Alert Policy Parameters should match",
                "{cron=0 1 0 1 0, scalingTarget=10, adjustmentType=NODE_COUNT, timeZone=GMT}",
                actual.getAutoscaleTriggerDetails().getAutoscalingPolicyDefinition().getAutoscalePolicyParameters());
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

        underTest.reportAutoscalingConfigChanged("testUserCrn", cluster);

        ArgumentCaptor<UsageProto.CDPDatahubAutoscaleConfigChanged> captor = ArgumentCaptor.forClass(UsageProto.CDPDatahubAutoscaleConfigChanged.class);
        verify(usageReporter, times(1)).cdpDatahubAutoscaleConfigChanged(captor.capture());
        UsageProto.CDPDatahubAutoscaleConfigChanged actual = captor.getValue();

        assertEquals("UserCrn should match", "testUserCrn", actual.getUserCrn());
        assertEquals("CluterName should match", "testCluster", actual.getClusterName());
        assertEquals("CluterCrn should match", "testStackCrn", actual.getClusterCrn());
        assertEquals("AccountId should match", "testAccount", actual.getAccountId());
        assertEquals("Policy Count should match", 2, actual.getAutoscalingPolicyDefinitionCount());
    }
}
