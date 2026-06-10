package com.sequenceiq.cloudbreak.common.tx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@ExtendWith(MockitoExtension.class)
class HibernateTransactionInterceptorTest {

    @Mock
    private MetricService metricService;

    @Mock
    private Transaction transaction;

    private HibernateTransactionInterceptor underTest;

    @BeforeEach
    void setUp() {
        underTest = new HibernateTransactionInterceptor();
        HibernateCircuitBreakerConfigProvider.init(null);
    }

    @Test
    void testTransactionMetricsRecordedWithOkOutcome() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(metricService);

            underTest.afterTransactionBegin(transaction);
            underTest.afterTransactionCompletion(transaction);

            verify(metricService).recordTimerMetric(eq(HibernateMetricType.TRANSACTION_DURATION),
                    any(Duration.class), eq("outcome"), eq("ok"));
            verify(metricService, never()).incrementMetricCounter(HibernateMetricType.TRANSACTION_WARNING);
        }
    }

    @Test
    void testTransactionMetricsRecordedWithWarningOutcome() throws InterruptedException {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class);
                MockedStatic<HibernateCircuitBreakerConfigProvider> configProvider = mockStatic(HibernateCircuitBreakerConfigProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(metricService);
            configProvider.when(HibernateCircuitBreakerConfigProvider::getMaxTransactionTimeThreshold).thenReturn(0L);

            underTest.afterTransactionBegin(transaction);
            Thread.sleep(2);
            underTest.afterTransactionCompletion(transaction);

            verify(metricService).recordTimerMetric(eq(HibernateMetricType.TRANSACTION_DURATION),
                    any(Duration.class), eq("outcome"), eq("warning"));
            verify(metricService).incrementMetricCounter(HibernateMetricType.TRANSACTION_WARNING);
        }
    }

    @Test
    void testNoMetricsRecordedWhenMetricServiceIsNull() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(null);

            underTest.afterTransactionBegin(transaction);
            underTest.afterTransactionCompletion(transaction);

            verify(metricService, never()).recordTimerMetric(any(HibernateMetricType.class), any(Duration.class), any(), any());
            verify(metricService, never()).incrementMetricCounter(any(HibernateMetricType.class));
        }
    }

    @Test
    void testNoMetricsRecordedWhenTransactionNotTracked() {
        try (MockedStatic<HibernateMetricsProvider> metricsProvider = mockStatic(HibernateMetricsProvider.class)) {
            metricsProvider.when(HibernateMetricsProvider::getMetricService).thenReturn(metricService);

            underTest.afterTransactionCompletion(transaction);

            verify(metricService, never()).recordTimerMetric(any(HibernateMetricType.class), any(Duration.class), any(), any());
        }
    }
}
