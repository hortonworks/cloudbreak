package com.sequenceiq.cloudbreak.service.metrics;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.type.metric.Metric;

public interface MetricService {
    void submit(Metric metric, double value);

    void submit(Metric metric, double value, Map<String, String> labels);

    void initMicrometerMetricCounter(Metric metric);

    void incrementMetricCounter(Metric metric);
}
