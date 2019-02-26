package com.sequenceiq.periscope.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.domain.MetricType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

@Service
public class MetricService {

    private static final String METRIC_PREFIX = "periscope.";

    private final Map<String, Double> gaugeCache = new HashMap<>();

    @PostConstruct
    protected void init() {
        Arrays.stream(MetricType.values())
                .filter(x -> !gaugeMetric(x))
                .forEach(this::initCounter);

        Arrays.stream(MetricType.values())
                .filter(this::gaugeMetric)
                .forEach(x -> submitGauge(x, 0));
    }

    private boolean gaugeMetric(MetricType x) {
        return x.getMetricName().contains("state") || x.getMetricName().contains("leader") || x.getMetricName().contains("threadpool");
    }

    public void incrementCounter(MetricType metric) {
        try (Counter counter = Metrics.counter(METRIC_PREFIX + metric.getMetricName().toLowerCase())) {
            counter.increment();
        }
    }

    public void submitGauge(MetricType metric, double value) {
        String metricName = METRIC_PREFIX + metric.getMetricName().toLowerCase();
        gaugeCache.put(metricName, value);

        Metrics.gauge(metricName, Collections.emptyList(), gaugeCache, cache -> cache.getOrDefault(metricName, 0.0d));
    }

    private void initCounter(MetricType metric) {
        try (Counter counter = Metrics.counter(METRIC_PREFIX + metric.getMetricName().toLowerCase())) {
            counter.increment(0);
        }
    }
}
