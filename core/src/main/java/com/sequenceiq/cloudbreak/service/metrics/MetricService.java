package com.sequenceiq.cloudbreak.service.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

@Service
public class MetricService {

    private static final String METRIC_PREFIX = "cloudbreak.";

    private final Map<String, Double> gaugeCache = new HashMap<>();

    @PostConstruct
    public void init() {
        for (MetricType metricType : MetricType.values()) {
            String metricName = metricType.getMetricName();
            if (metricName.startsWith("stack") || metricName.startsWith("cluster")) {
                initMicrometerMetricCounter(metricName, CloudConstants.AWS);
                initMicrometerMetricCounter(metricName, CloudConstants.AZURE);
                initMicrometerMetricCounter(metricName, CloudConstants.GCP);
                initMicrometerMetricCounter(metricName, CloudConstants.OPENSTACK);
                initMicrometerMetricCounter(metricName, CloudConstants.YARN);
            }
        }
    }

    /**
     * Increment a counter based metric.
     *
     * @param metric Metric name
     */
    public void incrementMetricCounter(MetricType metric) {
        incrementMetricCounter(metric.getMetricName());
    }

    /**
     * Increment a counter based metric. If the stack is provided then the metric's name
     * will be extended with the cloud platform. If the stack is null the original metric name will be used.
     *
     * @param metric Metric name
     * @param stack  Stack, used to determine the cloud platform
     */
    public void incrementMetricCounter(MetricType metric, StackView stack) {
        if (stack != null && stack.getPlatformVariant() != null) {
            incrementMetricCounter(getMetricNameWithPlatform(metric, stack.cloudPlatform()));
        } else {
            incrementMetricCounter(metric.getMetricName());
        }
    }

    /**
     * Increment a counter based metric. If the stack is provided then the metric's name
     * will be extended with the cloud platform. If the stack is null the original metric name will be used.
     *
     * @param metric Metric name
     * @param stack  Stack, used to determine the cloud platform
     */
    public void incrementMetricCounter(MetricType metric, Stack stack) {
        if (stack != null && stack.getPlatformVariant() != null) {
            incrementMetricCounter(getMetricNameWithPlatform(metric, stack.cloudPlatform()));
        } else {
            incrementMetricCounter(metric.getMetricName());
        }
    }

    /**
     * Set the specified gauge value.
     *
     * @param metric Metric name
     * @param value  Metric value
     */
    public void submit(String metric, double value) {
        submit(metric, value, new HashMap<>());
    }

    public void submit(String metric, double value, Map<String, String> labels) {
        String metricName = METRIC_PREFIX + metric.toLowerCase();
        gaugeCache.put(metricName, value);

        Iterable<Tag> tags = labels.entrySet().stream().map(label -> Tag.of(label.getKey(), label.getValue().toLowerCase())).collect(Collectors.toList());
        Metrics.gauge(metricName, tags, gaugeCache, cache -> cache.getOrDefault(metricName, 0.0d));
    }

    private void incrementMetricCounter(String metric) {
        try (Counter counter = Metrics.counter(METRIC_PREFIX + metric.toLowerCase())) {
            counter.increment();
        }
    }

    private void initMicrometerMetricCounter(String metric, String cloudPlatform) {
        try (Counter counter = Metrics.counter(METRIC_PREFIX + getMetricNameWithPlatform(metric, cloudPlatform))) {
            counter.increment(0);
        }
    }

    private String getMetricNameWithPlatform(MetricType metric, String cloudPlatform) {
        return getMetricNameWithPlatform(metric.getMetricName(), cloudPlatform);
    }

    private String getMetricNameWithPlatform(String metric, String cloudPlatform) {
        return metric + '.' + cloudPlatform.toLowerCase();
    }
}
