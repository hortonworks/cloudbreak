package com.sequenceiq.cloudbreak.cloud.aws.common.metrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.internal.DefaultMetricCollection;
import software.amazon.awssdk.metrics.internal.DefaultMetricRecord;

@ExtendWith(MockitoExtension.class)
class AwsMetricPublisherTest {

    @Mock
    private MetricService metricService;

    @InjectMocks
    private AwsMetricPublisher underTest;

    @Test
    void publishShouldReturnWhenMetricsIsEmpty() {
        underTest.publish(metricCollection(new HashMap<>()));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenServiceIdMissing() {
        underTest.publish(metricCollection(Map.of(CoreMetric.OPERATION_NAME, List.of(new DefaultMetricRecord<>(CoreMetric.OPERATION_NAME, "operationName")))));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenOperationNameMissing() {
        underTest.publish(metricCollection(Map.of(CoreMetric.SERVICE_ID, List.of(new DefaultMetricRecord<>(CoreMetric.SERVICE_ID, "serviceId")))));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenOperationNameIsNull() {
        underTest.publish(metricCollection(Map.of(CoreMetric.OPERATION_NAME, List.of(new DefaultMetricRecord<>(CoreMetric.OPERATION_NAME, null)))));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenServiceIdIsNull() {
        underTest.publish(metricCollection(Map.of(CoreMetric.SERVICE_ID, List.of(new DefaultMetricRecord<>(CoreMetric.SERVICE_ID, null)))));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenNoMetricWithExistingValuePresents() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_DURATION, null)));
        underTest.publish(metricCollection(metrics));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenNoMetricWithMetricValuePresents() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_DURATION, List.of(new DefaultMetricRecord<>(null, "value")));
        underTest.publish(metricCollection(metrics));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenNoKnownMetricPresents() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.MARSHALLING_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.MARSHALLING_DURATION, Duration.ofMinutes(1))));
        underTest.publish(metricCollection(metrics));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenApiCallSuccessfulMetricSucceeds() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_SUCCESSFUL, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_SUCCESSFUL, Boolean.TRUE)));
        underTest.publish(metricCollection(metrics));
        verifyNoInteractions(metricService);
    }

    @Test
    void publishShouldReturnWhenRecordingMetricFails() {
        doThrow(new RuntimeException("failed")).when(metricService).incrementMetricCounter(anyString(), any());
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_SUCCESSFUL, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_SUCCESSFUL, Boolean.FALSE)));
        metrics.put(CoreMetric.API_CALL_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_DURATION, Duration.ofMinutes(1))));
        underTest.publish(metricCollection(metrics));
        verify(metricService, times(1)).incrementMetricCounter(eq("aws_api_call_failed_total"), any(String[].class));
        verifyNoMoreInteractions(metricService);
    }

    @Test
    void publishShouldRecordApiCallFailedWhenApiCallSuccessfulMetricFailed() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_SUCCESSFUL, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_SUCCESSFUL, Boolean.FALSE)));
        underTest.publish(metricCollection(metrics));
        verify(metricService, only()).incrementMetricCounter(eq("aws_api_call_failed_total"), any(String[].class));
    }

    @Test
    void publishShouldRecordKnownDurationTypeMetric() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_DURATION, Duration.ofMinutes(1))));
        underTest.publish(metricCollection(metrics));
        verify(metricService, only()).recordTimerMetric(eq("aws_api_call_duration"), eq(Duration.ofMinutes(1)), any(String[].class));
    }

    @Test
    void publishShouldRecordKnownNumberTypeMetric() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.RETRY_COUNT, List.of(new DefaultMetricRecord<>(CoreMetric.RETRY_COUNT, Integer.valueOf(10))));
        underTest.publish(metricCollection(metrics));
        verify(metricService, only()).incrementMetricCounter(eq("aws_retry_count"), eq(10.0), any(String[].class));
    }

    @Test
    void publishShouldRecordKnownChildrenMetric() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        underTest.publish(metricCollection(metrics,
                List.of(metricCollection(Map.of(CoreMetric.RETRY_COUNT, List.of(new DefaultMetricRecord<>(CoreMetric.RETRY_COUNT, Integer.valueOf(10))))))));
        verify(metricService, only()).incrementMetricCounter(eq("aws_retry_count"), eq(10.0), any(String[].class));
    }

    @Test
    void publishShouldRecordAllKnownMetrics() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = defaultMetrics();
        metrics.put(CoreMetric.API_CALL_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_DURATION, Duration.ofMinutes(1))));
        metrics.put(CoreMetric.API_CALL_SUCCESSFUL, List.of(new DefaultMetricRecord<>(CoreMetric.API_CALL_SUCCESSFUL, Boolean.FALSE)));
        metrics.put(CoreMetric.MARSHALLING_DURATION, List.of(new DefaultMetricRecord<>(CoreMetric.MARSHALLING_DURATION, Duration.ofMinutes(1))));
        underTest.publish(metricCollection(metrics,
                List.of(metricCollection(Map.of(CoreMetric.RETRY_COUNT, List.of(new DefaultMetricRecord<>(CoreMetric.RETRY_COUNT, Integer.valueOf(10))))))));
        verify(metricService, times(1)).recordTimerMetric(eq("aws_api_call_duration"), eq(Duration.ofMinutes(1)), any(String[].class));
        verify(metricService, times(1)).incrementMetricCounter(eq("aws_api_call_failed_total"), any(String[].class));
        verify(metricService, times(1)).incrementMetricCounter(eq("aws_retry_count"), eq(10.0), any(String[].class));
        verifyNoMoreInteractions(metricService);
    }

    private Map<SdkMetric<?>, List<MetricRecord<?>>> defaultMetrics() {
        Map<SdkMetric<?>, List<MetricRecord<?>>> defaultMetrics = new HashMap<>();
        defaultMetrics.put(CoreMetric.OPERATION_NAME, List.of(new DefaultMetricRecord<>(CoreMetric.OPERATION_NAME, "operationName")));
        defaultMetrics.put(CoreMetric.SERVICE_ID, List.of(new DefaultMetricRecord<>(CoreMetric.SERVICE_ID, "serviceId")));
        return defaultMetrics;
    }

    private MetricCollection metricCollection(Map<SdkMetric<?>, List<MetricRecord<?>>> metrics) {
        return metricCollection(metrics, null);
    }

    private MetricCollection metricCollection(Map<SdkMetric<?>, List<MetricRecord<?>>> metrics, List<MetricCollection> children) {
        return new DefaultMetricCollection("metricCollection", metrics, children);
    }

}
