package com.sequenceiq.periscope.service;

import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.repository.LoadAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;

public class AlertServiceTest {

    @InjectMocks
    AlertService underTest;

    @Mock
    ClusterService clusterService;

    @Mock
    LoadAlertRepository loadAlertRepository;

    @Mock
    TimeAlertRepository timeAlertRepository;

    @Mock
    Cluster mockCluster;

    @Mock
    LoadAlert mockLoadAlert;

    @Mock
    TimeAlert mockTimeAlert;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateLoadAlert() {
        Long clusterId = 10L;
        LoadAlert testAlert = getLoadAlert();
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);
        when(clusterService.save(mockCluster)).thenReturn(mockCluster);
        when(loadAlertRepository.save(testAlert)).thenReturn(mockLoadAlert);

        LoadAlert response = underTest.createLoadAlert(clusterId, testAlert);

        assertNotNull("LoadAlert should not be null", response);
        assertNotNull("LoadAlert' cluster should not be null", testAlert.getCluster());
        verify(loadAlertRepository).save(any(LoadAlert.class));
        verify(clusterService).findById(clusterId);
        verify(clusterService).save(mockCluster);
    }

    @Test
    public void testUpdateLoadAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;
        LoadAlert testAlert = getLoadAlert();

        when(loadAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockLoadAlert);
        when(mockLoadAlert.getScalingPolicy()).thenReturn(new ScalingPolicy());
        when(loadAlertRepository.save(mockLoadAlert)).thenReturn(mockLoadAlert);

        LoadAlert response = underTest.updateLoadAlert(clusterId, alertId, testAlert);
        assertNotNull("LoadAlert should not be null", response);

        verify(mockLoadAlert).setName(anyString());
        verify(mockLoadAlert).setDescription(anyString());
        verify(mockLoadAlert, times(4)).getScalingPolicy();
        verify(loadAlertRepository).save(mockLoadAlert);
    }

    @Test
    public void testDeleteLoadAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;

        when(loadAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockLoadAlert);
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);

        underTest.deleteLoadAlert(clusterId, alertId);

        verify(mockCluster).setLoadAlerts(any(Set.class));
        verify(loadAlertRepository).delete(mockLoadAlert);
        verify(clusterService).save(mockCluster);
    }

    @Test
    public void testCreateTimeAlert() {
        Long clusterId = 10L;
        TimeAlert testAlert = getTimeAlert();
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);
        when(clusterService.save(mockCluster)).thenReturn(mockCluster);
        when(timeAlertRepository.save(testAlert)).thenReturn(mockTimeAlert);

        TimeAlert response = underTest.createTimeAlert(clusterId, testAlert);

        assertNotNull("TestAlert should not be null", response);
        assertNotNull("TestAlert' cluster should not be null", testAlert.getCluster());
        verify(timeAlertRepository).save(any(TimeAlert.class));
        verify(clusterService).findById(clusterId);
        verify(clusterService).save(mockCluster);
        verify(mockCluster).addTimeAlert(mockTimeAlert);
    }

    @Test
    public void testUpdateTimeAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;
        TimeAlert testAlert = getTimeAlert();

        when(timeAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockTimeAlert);
        when(mockTimeAlert.getScalingPolicy()).thenReturn(new ScalingPolicy());
        when(timeAlertRepository.save(mockTimeAlert)).thenReturn(mockTimeAlert);

        TimeAlert response = underTest.updateTimeAlert(clusterId, alertId, testAlert);
        assertNotNull("TimeAlert should not be null", response);

        verify(mockTimeAlert).setName(anyString());
        verify(mockTimeAlert).setDescription(anyString());
        verify(mockTimeAlert).setCron(anyString());
        verify(mockTimeAlert).setTimeZone(anyString());
        verify(mockTimeAlert, times(4)).getScalingPolicy();
        verify(timeAlertRepository).save(mockTimeAlert);
    }

    @Test
    public void testDeleteTimeAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;

        when(timeAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockTimeAlert);
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);

        underTest.deleteTimeAlert(clusterId, alertId);

        verify(mockCluster).setTimeAlerts(any(Set.class));
        verify(timeAlertRepository).delete(mockTimeAlert);
        verify(clusterService).save(mockCluster);
    }

    private ScalingPolicy getScalingPolicy() {
        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setName("loadalertpolicy");
        scalingPolicy.setAdjustmentType(NODE_COUNT);
        scalingPolicy.setHostGroup("compute");
        scalingPolicy.setScalingAdjustment(10);
        return scalingPolicy;
    }

    private LoadAlert getLoadAlert() {
        LoadAlert testAlert = new LoadAlert();
        testAlert.setName("test");
        testAlert.setDescription("test desc");
        testAlert.setScalingPolicy(getScalingPolicy());
        return testAlert;
    }

    private TimeAlert getTimeAlert() {
        TimeAlert testAlert = new TimeAlert();
        testAlert.setName("test");
        testAlert.setDescription("test desc");
        testAlert.setCron("cron string");
        testAlert.setTimeZone("GMT");
        testAlert.setScalingPolicy(getScalingPolicy());
        return testAlert;
    }
}
