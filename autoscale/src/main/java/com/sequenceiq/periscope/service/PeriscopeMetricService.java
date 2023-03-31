package com.sequenceiq.periscope.service;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.periscope.domain.MetricType;

@Primary
@Service("MetricService")
public class PeriscopeMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "periscope";

    @PostConstruct
    protected void init() {
        Arrays.stream(MetricType.values())
                .filter(m -> !gaugeMetric(m))
                .forEach(this::initMicrometerMetricCounter);

        Arrays.stream(MetricType.values())
                .filter(this::gaugeMetric)
                .forEach(m -> gauge(m, 0));
    }

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.of(METRIC_PREFIX);
    }
}
