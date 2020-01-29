package com.sequenceiq.freeipa.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "freeipa";

    public void incrementMetricCounter(MetricType metricType, Stack stack) {
        incrementMetricCounter(metricType,
                FreeIpaMetricTag.CCM_ENABLED.name(), Boolean.toString(stack.getUseCcm()),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.getCloudPlatform());
    }

    public void incrementMetricCounter(MetricType metricType, Stack stack, Exception exception) {
        incrementMetricCounter(metricType,
                FreeIpaMetricTag.CCM_ENABLED.name(), Boolean.toString(stack.getUseCcm()),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name(),
                MetricTag.EXCEPTION_TYPE.name(), exception.getClass().getName(),
                MetricTag.CLOUD_PROVIDER.name(), stack.getCloudPlatform());
    }

    private String getMetricNameWithPlatform(MetricType metric, String cloudPlatform) {
        return String.format("%s.%s.%s", METRIC_PREFIX, metric.getMetricName(), cloudPlatform.toLowerCase());
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}