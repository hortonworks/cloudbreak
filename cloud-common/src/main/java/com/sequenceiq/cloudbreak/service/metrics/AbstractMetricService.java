package com.sequenceiq.cloudbreak.service.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.common.type.metric.Metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

public abstract class AbstractMetricService implements MetricService {

    private final Map<String, Double> gaugeCache = new HashMap<>();

    /**
     * Set the specified gauge value.
     *
     * @param metric Metric name
     * @param value  Metric value
     */
    @Override
    public void submit(Metric metric, double value) {
        submit(metric, value, Collections.emptyMap());
    }

    @Override
    public void submit(Metric metric, double value, Map<String, String> labels) {
        String metricName = getMetricName(metric);
        gaugeCache.put(metricName, value);

        Iterable<Tag> tags = labels.entrySet().stream().map(label -> Tag.of(label.getKey(), label.getValue().toLowerCase())).collect(Collectors.toList());
        Metrics.gauge(metricName, tags, gaugeCache, cache -> cache.getOrDefault(metricName, 0.0d));
    }

    @Override
    public void initMicrometerMetricCounter(Metric metric) {
        initMicrometerMetricCounter(getMetricName(metric));
    }

    protected void initMicrometerMetricCounter(String metric) {
        Counter counter = Metrics.counter(metric);
        counter.increment(0);
    }

    @Override
    public void incrementMetricCounter(Metric metric) {
        incrementMetricCounter(getMetricName(metric));
    }

    protected void incrementMetricCounter(String metric) {
        Counter counter = Metrics.counter(metric);
        counter.increment();
    }

    protected boolean gaugeMetric(Metric metric) {
        return metric.getMetricName().contains("state") || metric.getMetricName().contains("leader") || metric.getMetricName().contains("threadpool");
    }

    private String getMetricName(Metric metric) {
        return getMetricPrefix() + '.' + metric.getMetricName().toLowerCase();
    }

    protected abstract String getMetricPrefix();
}
