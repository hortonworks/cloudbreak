package com.sequenceiq.cloudbreak.common.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.AtomicDouble;
import com.sequenceiq.cloudbreak.common.metrics.type.Metric;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.common.service.TransactionMetricsContext;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public abstract class AbstractMetricService implements MetricService {

    private final ConcurrentMap<GaugeMetricKey, AtomicDouble> gaugeMetricMap = new ConcurrentHashMap<>();

    /**
     * Set the specified gauge value.
     *
     * @param metric Metric name
     * @param value  Metric value
     */
    @Override
    public void gauge(Metric metric, double value) {
        gauge(metric, value, Collections.emptyMap());
    }

    @Override
    public void gauge(Metric metric, double value, Map<String, String> tagsMap) {
        GaugeMetricKey key = new GaugeMetricKey(getMetricName(metric), tagsMap);
        gaugeMetricMap.computeIfAbsent(key, gaugeKey -> {
            Iterable<Tag> tags = gaugeKey.tags().entrySet().stream()
                    .map(label -> Tag.of(label.getKey(), label.getValue().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
            return Metrics.gauge(gaugeKey.metricName(), tags, new AtomicDouble(value));
        }).set(value);
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
        incrementMetricCounter(getMetricName(metric), 1.0, tags);
    }

    @Override
    public void incrementMetricCounter(String metric, String... tags) {
        incrementMetricCounter(metric, 1.0, tags);
    }

    @Override
    public void incrementMetricCounter(String metric, double amount, String... tags) {
        Counter counter = Metrics.counter(metric, tags);
        counter.increment(amount);
    }

    @Override
    public <T, U> Map<T, U> gaugeMapSize(Metric metric, Map<T, U> map) {
        return Metrics.gaugeMapSize(getMetricName(metric), Tags.empty(), map);
    }

    protected boolean gaugeMetric(Metric metric) {
        return metric.getMetricName().contains("state") || metric.getMetricName().contains("leader") || metric.getMetricName().contains("threadpool");
    }

    protected void recordTimer(long millisToRecord, Metric metric, String... tags) {
        Timer.builder(getMetricName(metric))
                .tags(tags)
                .register(Metrics.globalRegistry)
                .record(millisToRecord, TimeUnit.MILLISECONDS);
    }

    private String getMetricName(Metric metric) {
        if (getMetricPrefix().isPresent()) {
            return getMetricPrefix().get() + '.' + metric.getMetricName().toLowerCase(Locale.ROOT);
        } else {
            return metric.getMetricName().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public void recordTimerMetric(Metric metric, Duration duration, String... tags) {
        Timer timer = Metrics.timer(getMetricName(metric), tags);
        timer.record(duration);
    }

    @Override
    public void recordTimerMetric(String metric, Duration duration, String... tags) {
        Timer timer = Metrics.timer(metric, tags);
        timer.record(duration);
    }

    @Override
    public <T> void registerGaugeMetric(Metric metric, T object, ToDoubleFunction<T> valueFunction, Map<String, String> tags) {
        List<Tag> tagList = getTagList(tags);
        Metrics.gauge(getMetricName(metric), tagList, object, valueFunction);
    }

    private List<Tag> getTagList(Map<String, String> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.entrySet().stream().map(e -> Tag.of(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public void recordTransactionTime(TransactionMetricsContext transactionMetricsContext, long duration) {
        recordTimer(duration, MetricType.DB_TRANSACTION_ID);
    }

    protected abstract Optional<String> getMetricPrefix();

    private record GaugeMetricKey(String metricName, Map<String, String> tags) {
    }
}
