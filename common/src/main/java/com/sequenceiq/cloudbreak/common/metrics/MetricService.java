package com.sequenceiq.cloudbreak.common.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsContext;

public interface MetricService {
    void gauge(Metric metric, double value);

    void gauge(Metric metric, double value, Map<String, String> tags);

    void initMicrometerMetricCounter(Metric metric);

    void incrementMetricCounter(Metric metric, String... tags);

    void recordTimer(long duration, Metric metric, String... tags);

    void incrementMetricCounter(String metric, String... tags);

    void incrementMetricCounter(String metric, double amount, String... tags);

    <T, U> Map<T, U> gaugeMapSize(Metric metric, Map<T, U> map);

    void recordTimerMetric(Metric metric, Duration duration, String... tags);

    void recordTimerMetric(String metric, Duration duration, String... tags);

    <T> void registerGaugeMetric(Metric metric, T object, ToDoubleFunction<T> valueFunction, Map<String, String> tags);

    void recordTransactionTime(TransactionMetricsContext transactionMetricsContext, long duration);
}
