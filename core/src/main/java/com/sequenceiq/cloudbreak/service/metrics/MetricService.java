package com.sequenceiq.cloudbreak.service.metrics;

import javax.inject.Inject;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class MetricService {

    private static final String METRIC_PREFIX = "cloudbreak.";

    @Inject
    private CounterService counterService;

    @Inject
    private GaugeService gaugeService;

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
    public void incrementMetricCounter(MetricType metric, Stack stack) {
        if (stack != null && stack.getPlatformVariant() != null) {
            incrementMetricCounter(getMetricNameWithPlatform(metric, stack));
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
        gaugeService.submit(METRIC_PREFIX + metric.toLowerCase(), value);
    }

    private void incrementMetricCounter(String metric) {
        counterService.increment(METRIC_PREFIX + metric.toLowerCase());
    }

    private String getMetricNameWithPlatform(MetricType metric, Stack stack) {
        return getMetricNameWithPlatform(metric.getMetricName(), stack);
    }

    private String getMetricNameWithPlatform(String metric, Stack stack) {
        return metric + '.' + stack.cloudPlatform().toLowerCase();
    }
}
