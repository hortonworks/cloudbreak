package com.sequenceiq.periscope.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.NotFoundException;

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
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Mock
    private TimeAlertResponseConverter timeAlertResponseConverter;

    private DateService dateService = new DateService();

    private Long clusterId = 10L;

    private Long alertId = 20L;

    @Before
    public void setup() {
        underTest.setDateService(dateService);
    }

    @Test(expected = NotFoundException.class)
    public void testLoadAlertUpdateNotFound() throws Exception {
        LoadAlertRequest request = new LoadAlertRequest();

        when(clusterService.findStackCrnById(clusterId)).thenReturn("test");
        when(alertService.findLoadAlertByCluster(clusterId, alertId)).thenReturn(null);

        underTest.updateLoadAlert(clusterId, alertId, request);
    }

    @Test
    public void testLoadAlertUpdate() throws Exception {
        LoadAlertRequest request = new LoadAlertRequest();
        LoadAlert alert = new LoadAlert();

        when(alertService.findLoadAlertByCluster(clusterId, alertId)).thenReturn(alert);
        when(loadAlertRequestConverter.convert(request)).thenCallRealMethod();

        underTest.updateLoadAlert(clusterId, alertId, request);
        verify(alertService).updateLoadAlert(anyLong(), anyLong(), any(LoadAlert.class));
    }

    @Test
    public void testLoadAlertCreate() throws Exception {
        LoadAlertRequest request = new LoadAlertRequest();
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        Cluster clusterMock = mock(Cluster.class);
        when(alertService.getLoadAlertsForClusterHostGroup(clusterId, "compute")).thenReturn(Set.of());
        when(loadAlertRequestConverter.convert(request)).thenReturn(new LoadAlert());
        when(clusterService.findById(clusterId)).thenReturn(clusterMock);
        when(clusterMock.getTunnel()).thenReturn(Tunnel.CLUSTER_PROXY);

        underTest.createLoadAlert(clusterId, request);
        verify(alertService).createLoadAlert(anyLong(), any(LoadAlert.class));
    }

    @Test(expected = BadRequestException.class)
    public void testLoadAlertCreateDuplicate() throws Exception {
        LoadAlertRequest request = new LoadAlertRequest();
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        Cluster clusterMock = mock(Cluster.class);
        when(clusterService.findById(clusterId)).thenReturn(clusterMock);
        when(clusterMock.getTunnel()).thenReturn(Tunnel.CLUSTER_PROXY);
        when(alertService.getLoadAlertsForClusterHostGroup(clusterId, "compute")).thenReturn(Set.of(new LoadAlert()));

        underTest.createLoadAlert(clusterId, request);
    }

    @Test(expected = BadRequestException.class)
    public void testLoadAlertCreateTunnelDirect() throws Exception  {
        LoadAlertRequest request = new LoadAlertRequest();
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        Cluster clusterMock = mock(Cluster.class);
        when(clusterService.findById(clusterId)).thenReturn(clusterMock);
        when(clusterMock.getTunnel()).thenReturn(Tunnel.DIRECT);

        underTest.createLoadAlert(clusterId, request);
    }

    @Test
    public void testTimeAlertCreate() throws Exception {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        when(timeAlertRequestConverter.convert(request)).thenReturn(new TimeAlert());

        underTest.createTimeAlert(clusterId, request);
        verify(alertService).createTimeAlert(anyLong(), any(TimeAlert.class));
    }

    @Test(expected = NotFoundException.class)
    public void testTimeAlertUpdateNotFound() throws Exception {
        TimeAlertRequest request = new TimeAlertRequest();

        when(clusterService.findStackCrnById(clusterId)).thenReturn("test");
        when(alertService.findTimeAlertByCluster(clusterId, alertId)).thenReturn(null);

        underTest.updateTimeAlert(clusterId, alertId, request);
    }

    @Test
    public void testTimeAlertUpdate() throws Exception {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);
        TimeAlert alert = new TimeAlert();

        when(timeAlertRequestConverter.convert(request)).thenReturn(alert);
        when(alertService.findTimeAlertByCluster(clusterId, alertId)).thenReturn(alert);

        underTest.updateTimeAlert(clusterId, alertId, request);
        verify(alertService).updateTimeAlert(anyLong(), anyLong(), any(TimeAlert.class));
    }
}
