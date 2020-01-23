package com.sequenceiq.cloudbreak.common.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

public abstract class AbstractMetricService implements MetricService {

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
        Iterable<Tag> tags = labels.entrySet().stream().map(label -> Tag.of(label.getKey(), label.getValue().toLowerCase())).collect(Collectors.toList());
        Metrics.gauge(metricName, tags, value);
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
    public void incrementMetricCounter(Metric metric, String... tags) {
        incrementMetricCounter(getMetricName(metric), tags);
    }

    protected void incrementMetricCounter(String metric, String... tags) {
        Counter counter = Metrics.counter(metric, tags);
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
