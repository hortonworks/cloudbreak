package com.sequenceiq.thunderhead.service;

import java.time.Duration;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsContext;

@Service("CommonMetricService")
public class MockMetricService implements MetricService {

    @Override
    public void gauge(Metric metric, double value) {

    }

    @Override
    public void gauge(Metric metric, double value, Map<String, String> tags) {

    }

    @Override
    public void initMicrometerMetricCounter(Metric metric) {

    }

    @Override
    public void incrementMetricCounter(Metric metric, String... tags) {

    }

    @Override
    public void recordTimer(long duration, Metric metric, String... tags) {

    }

    @Override
    public void incrementMetricCounter(String metric, String... tags) {

    }

    @Override
    public void incrementMetricCounter(String metric, double amount, String... tags) {

    }

    @Override
    public <T, U> Map<T, U> gaugeMapSize(Metric metric, Map<T, U> map) {
        return Map.of();
    }

    @Override
    public void recordTimerMetric(Metric metric, Duration duration, String... tags) {

    }

    @Override
    public void recordTimerMetric(String metric, Duration duration, String... tags) {

    }

    @Override
    public <T> void registerGaugeMetric(Metric metric, T object, ToDoubleFunction<T> valueFunction, Map<String, String> tags) {

    }

    @Override
    public void recordTransactionTime(TransactionMetricsContext transactionMetricsContext, long duration) {

    }
}
