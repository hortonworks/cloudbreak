package com.sequenceiq.cloudbreak.common.metrics;

import java.time.Duration;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public interface MetricService {
    void submit(Metric metric, double value);

    void submit(Metric metric, double value, Map<String, String> labels);

    void initMicrometerMetricCounter(Metric metric);

    void incrementMetricCounter(Metric metric, String... tags);

    <T, U> Map<T, U> gaugeMapSize(Metric metric, Map<T, U> map);

    void recordTimerMetric(Metric metric, Duration duration, String... tags);

    <T> void registerGaugeMetric(Metric metric, T object, ToDoubleFunction<T> valueFunction, Map<String, String> tags);
}
