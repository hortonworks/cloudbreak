package com.sequenceiq.periscope.service;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.metrics.AbstractMetricService;
import com.sequenceiq.periscope.domain.MetricType;

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
                .forEach(m -> submit(m, 0));
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}
