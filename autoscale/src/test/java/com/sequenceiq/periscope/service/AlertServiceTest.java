package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.periscope.api.model.AdjustmentType.NODE_COUNT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.repository.LoadAlertRepository;
import com.sequenceiq.periscope.repository.TimeAlertRepository;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @InjectMocks
    private AlertService underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LoadAlertRepository loadAlertRepository;

    @Mock
    private TimeAlertRepository timeAlertRepository;

    @Mock
    private Cluster mockCluster;

    @Mock
    private LoadAlert mockLoadAlert;

    @Mock
    private TimeAlert mockTimeAlert;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @BeforeEach
    void setup() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @Test
    void testCreateLoadAlert() {
        Long clusterId = 10L;
        LoadAlert testAlert = getLoadAlert();
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);
        when(clusterService.save(mockCluster)).thenReturn(mockCluster);
        when(loadAlertRepository.save(testAlert)).thenReturn(mockLoadAlert);

        LoadAlert response = doAs(TEST_USER_CRN, () -> underTest.createLoadAlert(clusterId, testAlert));

        assertNotNull(response, "LoadAlert should not be null");
        assertNotNull(testAlert.getCluster(), "LoadAlert' cluster should not be null");
        verify(loadAlertRepository).save(any(LoadAlert.class));
        verify(clusterService).findById(clusterId);
        verify(clusterService).save(mockCluster);
    }

    @Test
    void testUpdateLoadAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;
        LoadAlert testAlert = getLoadAlert();

        when(loadAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockLoadAlert);
        when(mockLoadAlert.getScalingPolicy()).thenReturn(new ScalingPolicy());
        when(loadAlertRepository.save(mockLoadAlert)).thenReturn(mockLoadAlert);

        LoadAlert response = doAs(TEST_USER_CRN, () -> underTest.updateLoadAlert(clusterId, alertId, testAlert));
        assertNotNull(response, "LoadAlert should not be null");

        verify(mockLoadAlert).setName(anyString());
        verify(mockLoadAlert).setDescription(anyString());
        verify(mockLoadAlert, times(4)).getScalingPolicy();
        verify(loadAlertRepository).save(mockLoadAlert);
    }

    @Test
    void testDeleteLoadAlert() {
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
    void testCreateTimeAlert() {
        Long clusterId = 10L;
        TimeAlert testAlert = getTimeAlert();
        when(clusterService.findById(clusterId)).thenReturn(mockCluster);
        when(clusterService.save(mockCluster)).thenReturn(mockCluster);
        when(timeAlertRepository.save(testAlert)).thenReturn(mockTimeAlert);

        TimeAlert response = doAs(TEST_USER_CRN, () -> underTest.createTimeAlert(clusterId, testAlert));

        assertNotNull(response, "TestAlert should not be null");
        assertNotNull(testAlert.getCluster(), "TestAlert' cluster should not be null");
        verify(timeAlertRepository).save(any(TimeAlert.class));
        verify(clusterService).findById(clusterId);
        verify(clusterService).save(mockCluster);
        verify(mockCluster).addTimeAlert(mockTimeAlert);
    }

    @Test
    void testUpdateTimeAlert() {
        Long clusterId = 10L;
        Long alertId = 20L;
        TimeAlert testAlert = getTimeAlert();

        when(timeAlertRepository.findByCluster(alertId, clusterId)).thenReturn(mockTimeAlert);
        when(mockTimeAlert.getScalingPolicy()).thenReturn(new ScalingPolicy());
        when(timeAlertRepository.save(mockTimeAlert)).thenReturn(mockTimeAlert);

        TimeAlert response = doAs(TEST_USER_CRN, () -> underTest.updateTimeAlert(clusterId, alertId, testAlert));
        assertNotNull(response, "TimeAlert should not be null");

        verify(mockTimeAlert).setName(anyString());
        verify(mockTimeAlert).setDescription(anyString());
        verify(mockTimeAlert).setCron(anyString());
        verify(mockTimeAlert).setTimeZone(anyString());
        verify(mockTimeAlert, times(4)).getScalingPolicy();
        verify(timeAlertRepository).save(mockTimeAlert);
    }

    @Test
    void testDeleteTimeAlert() {
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
