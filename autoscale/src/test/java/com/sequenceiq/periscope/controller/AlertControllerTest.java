package com.sequenceiq.periscope.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.converter.LoadAlertConfigurationRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.ScalingPolicyRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.NotFoundException;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@RunWith(MockitoJUnitRunner.class)
public class AlertControllerTest {

    @InjectMocks
    private AlertController underTest;

    @Mock
    private AlertService alertService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Mock
    private LoadAlertResponseConverter loadAlertResponseConverter;

    @Mock
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Mock
    private LoadAlertConfigurationRequestConverter loadAlertConfigurationRequestConverter;

    @Mock
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Mock
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    private DateService dateService = new DateService();

    private Long clusterId = 10L;

    private Long workspaceId = 10L;

    private Long alertId = 20L;

    @Before
    public void setup() {
        Whitebox.setInternalState(underTest, "supportedCloudPlatforms", Set.of("AWS", "AZURE"));
        underTest.setDateService(dateService);
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NotFoundException.class)
    public void testLoadAlertUpdateNotFound() {
        LoadAlertRequest request = getALoadAlertRequest();

        Optional<Cluster> aCluster = getACluster();
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);

        underTest.updateLoadAlert(clusterId, alertId, request);
    }

    @Test
    public void testLoadAlertUpdate() {
        LoadAlertRequest request = getALoadAlertRequest();
        LoadAlert alert = getALoadAlert();

        Optional<Cluster> aCluster = getACluster();
        aCluster.get().setLoadAlerts(Set.of(alert));
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        when(loadAlertRequestConverter.convert(request)).thenReturn(alert);

        underTest.updateLoadAlert(clusterId, alertId, request);
        verify(alertService).updateLoadAlert(anyLong(), anyLong(), any(LoadAlert.class));
    }

    @Test
    public void testLoadAlertCreate() {
        LoadAlertRequest request = getALoadAlertRequest();

        Optional<Cluster> aCluster = getACluster();
        when(loadAlertRequestConverter.convert(request)).thenReturn(getALoadAlert());
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.of("http://clusterproxy"));

        underTest.createLoadAlert(clusterId, request);
        verify(alertService).createLoadAlert(anyLong(), any(LoadAlert.class));
    }

    @Test(expected = BadRequestException.class)
    public void testLoadAlertCreateWhenClusterProxyNotRegistered() {
        LoadAlertRequest request = getALoadAlertRequest();

        Optional<Cluster> aCluster = getACluster();
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        when(clusterProxyConfigurationService.getClusterProxyUrl()).thenReturn(Optional.empty());

        underTest.createLoadAlert(clusterId, request);
        verify(alertService).createLoadAlert(anyLong(), any(LoadAlert.class));
    }

    @Test(expected = BadRequestException.class)
    public void testInvalidCloudPlatform() {
        LoadAlertRequest request = getALoadAlertRequest();
        Optional<Cluster> aCluster = getACluster();
        aCluster.get().setCloudPlatform("Yarn");

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);

        underTest.createLoadAlert(clusterId, request);
    }

    @Test(expected = BadRequestException.class)
    public void testLoadAlertCreateDuplicate() {
        LoadAlertRequest request = getALoadAlertRequest();

        Optional<Cluster> aCluster = getACluster();
        aCluster.get().setLoadAlerts(Set.of(getALoadAlert()));

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);

        underTest.createLoadAlert(clusterId, request);
    }

    @Test
    public void testTimeAlertCreate() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        Optional<Cluster> aCluster = getACluster();

        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        when(timeAlertRequestConverter.convert(request)).thenReturn(new TimeAlert());

        underTest.createTimeAlert(clusterId, request);
        verify(alertService).createTimeAlert(anyLong(), any(TimeAlert.class));
    }

    @Test(expected = NotFoundException.class)
    public void testTimeAlertUpdateNotFound() {
        TimeAlertRequest request = new TimeAlertRequest();

        Optional<Cluster> aCluster = getACluster();
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        underTest.updateTimeAlert(clusterId, alertId, request);
    }

    @Test
    public void testTimeAlertUpdate() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);
        TimeAlert alert = new TimeAlert();
        alert.setId(alertId);

        Optional<Cluster> aCluster = getACluster();
        aCluster.get().setTimeAlerts(Set.of(alert));
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspaceId);
        when(clusterService.findOneByClusterIdAndWorkspaceId(clusterId, workspaceId)).thenReturn(aCluster);
        when(timeAlertRequestConverter.convert(request)).thenReturn(alert);

        underTest.updateTimeAlert(clusterId, alertId, request);
        verify(alertService).updateTimeAlert(anyLong(), anyLong(), any(TimeAlert.class));
    }

    private LoadAlert getALoadAlert() {
        LoadAlert alert = new LoadAlert();
        alert.setId(alertId);
        LoadAlertConfiguration testConfiguration = new LoadAlertConfiguration();
        testConfiguration.setMaxResourceValue(200);
        testConfiguration.setMinResourceValue(0);
        alert.setLoadAlertConfiguration(testConfiguration);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup("compute");
        alert.setScalingPolicy(scalingPolicy);
        return alert;
    }

    private LoadAlertRequest getALoadAlertRequest() {
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        LoadAlertConfigurationRequest testConfiguration = new LoadAlertConfigurationRequest();
        testConfiguration.setMaxResourceValue(200);
        testConfiguration.setMinResourceValue(0);
        loadAlertRequest.setLoadAlertConfiguration(testConfiguration);
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        loadAlertRequest.setScalingPolicy(scalingPolicy);
        return loadAlertRequest;
    }

    private Optional<Cluster> getACluster() {
        Cluster cluster = new Cluster();
        cluster.setCloudPlatform("AWS");
        cluster.setTunnel(Tunnel.CLUSTER_PROXY);
        return Optional.of(cluster);
    }
}
