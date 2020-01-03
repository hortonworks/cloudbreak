package com.sequenceiq.periscope.monitor.evaluator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.api.model.AlertState;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.MetricAlert;

@RunWith(MockitoJUnitRunner.class)
public class MetricConditionTest {

    @Mock
    private AmbariClient ambariClient;

    @Mock
    private AmbariRequestLogging ambariRequestLogging;

    @InjectMocks
    private MetricCondition underTest;

    private MetricAlert metricAlert;

    @Before
    public void init() {
        metricAlert = new MetricAlert();
        metricAlert.setDefinitionName("AlertDEfinitionName");
        metricAlert.setName("AlertName");
        metricAlert.setAlertState(AlertState.CRITICAL);
        when(ambariRequestLogging.logging(any(), anyString())).thenCallRealMethod();
    }

    @Test
    public void testNoAlert() {
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(Collections.emptyList());
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertFalse(result);
    }

    @Test
    public void testOneAlertPeriodReached() {
        Map<String, Object> alert = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 80000);
        metricAlert.setPeriod(1);
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(Collections.singletonList(alert));
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertTrue(result);
    }

    @Test
    public void testOneAlertPeriodNotReached() {
        Map<String, Object> alert = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 1000);
        metricAlert.setPeriod(1);
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(Collections.singletonList(alert));
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertFalse(result);
    }

    @Test
    public void testTwoAlertOnePeriodReached() {
        Map<String, Object> alert1 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 80000);
        Map<String, Object> alert2 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 1000);
        metricAlert.setPeriod(1);
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(List.of(alert1, alert2));
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertTrue(result);
    }

    @Test
    public void testTwoAlertBothPeriodReached() {
        Map<String, Object> alert1 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 80000);
        Map<String, Object> alert2 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 90000);
        metricAlert.setPeriod(1);
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(List.of(alert1, alert2));
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertTrue(result);
    }

    @Test
    public void testTwoAlertNonePeriodReached() {
        Map<String, Object> alert1 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 8000);
        Map<String, Object> alert2 = Map.of(MetricCondition.ALERT_TS, System.currentTimeMillis() - 9000);
        metricAlert.setPeriod(1);
        when(ambariClient.getAlertByNameAndState(anyString(), anyString())).thenReturn(List.of(alert1, alert2));
        boolean result = underTest.isMetricAlertTriggered(ambariClient, metricAlert);
        assertFalse(result);
    }
}