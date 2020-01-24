package com.sequenceiq.environment.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;

@Service("MetricService")
public class EnvironmentMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "environment";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}
