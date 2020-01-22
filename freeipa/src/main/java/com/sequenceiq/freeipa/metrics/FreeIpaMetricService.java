package com.sequenceiq.freeipa.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;

@Service
public class FreeIpaMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "freeipa";

    private String getMetricNameWithPlatform(MetricType metric, String cloudPlatform) {
        return String.format("%s.%s.%s", METRIC_PREFIX, metric.getMetricName(), cloudPlatform.toLowerCase());
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}