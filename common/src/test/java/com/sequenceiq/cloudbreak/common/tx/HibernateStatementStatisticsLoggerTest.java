package com.sequenceiq.cloudbreak.common.tx;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@ExtendWith(MockitoExtension.class)
public class HibernateStatementStatisticsLoggerTest {

    @Mock
    private MetricService metricService;

    private HibernateStatementStatisticsLogger underTest;

    @BeforeEach
    public void setUp() {
        underTest = new HibernateStatementStatisticsLogger();
        HibernateCircuitBreakerConfigProvider.init(null);
    }

    @Test
    public void testNoWarning() {
        callSQL(500);
        underTest.end();

        String log = underTest.constructStatementStatisticLogline();
        assertThat(log, containsString("executing 500 JDBC statements"));
    }

    @Test
    void testWarning() {
        callSQL(501);
        underTest.end();
        String log = underTest.constructStatementStatisticLogline();
        assertThat(log, containsString("executing 501 JDBC statements"));
    }

    @Test
    void testMetricsRecordedForJdbcExecutionTimes() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(metricService);

            callSQL(10);
            underTest.end();

            verify(metricService).recordTimerMetric(eq(HibernateMetricType.JDBC_EXECUTE_DURATION),
                    any(Duration.class), eq("type"), eq("statement"));
            verify(metricService).recordTimerMetric(eq(HibernateMetricType.JDBC_EXECUTE_DURATION),
                    any(Duration.class), eq("type"), eq("prepare"));
            verify(metricService, never()).recordTimerMetric(eq(HibernateMetricType.JDBC_EXECUTE_DURATION),
                    any(Duration.class), eq("type"), eq("batch"));
            verify(metricService, never()).incrementMetricCounter(HibernateMetricType.STATEMENT_COUNT_WARNING);
        }
    }

    @Test
    void testStatementCountWarningMetricIncrementedWhenThresholdExceeded() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class);
                MockedStatic<HibernateCircuitBreakerConfigProvider> configProvider = mockStatic(HibernateCircuitBreakerConfigProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(metricService);
            configProvider.when(HibernateCircuitBreakerConfigProvider::getMaxStatementWarning).thenReturn(5);
            configProvider.when(HibernateCircuitBreakerConfigProvider::getMaxTimeWarning).thenReturn(1000L);

            callSQL(10);
            underTest.end();

            verify(metricService).incrementMetricCounter(HibernateMetricType.STATEMENT_COUNT_WARNING);
        }
    }

    @Test
    void testNoMetricsRecordedWhenMetricServiceIsNull() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(null);

            callSQL(10);
            underTest.end();

            verify(metricService, never()).recordTimerMetric(any(HibernateMetricType.class), any(Duration.class), any(), any());
            verify(metricService, never()).incrementMetricCounter(any(HibernateMetricType.class));
        }
    }

    private void callSQL(int count) {
        underTest.jdbcPrepareStatementStart();
        underTest.jdbcPrepareStatementEnd();

        for (int i = 0; i < count; i++) {
            underTest.jdbcExecuteStatementStart();
            underTest.jdbcExecuteStatementEnd();
        }
    }

}
