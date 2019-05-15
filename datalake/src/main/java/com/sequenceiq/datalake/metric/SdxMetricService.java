package com.sequenceiq.datalake.metric;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;

@Service("MetricService")
public class SdxMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "com.sequenceiq.sdx";

    private String getMetricNameWithPlatform(MetricType metric, String cloudPlatform) {
        return String.format("%s.%s.%s", METRIC_PREFIX, metric.getMetricName(), cloudPlatform.toLowerCase());
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}
