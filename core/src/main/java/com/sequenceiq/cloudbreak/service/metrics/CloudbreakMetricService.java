package com.sequenceiq.cloudbreak.service.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;

@Service("MetricService")
public class CloudbreakMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "cloudbreak";

    public void incrementMetricCounter(MetricType metric, Stack stack) {
        incrementMetricCounter(metric,
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
    }

    public void incrementMetricCounter(MetricType metric, Stack stack, Exception e) {
        incrementMetricCounter(metric,
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName(),
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
    }

    public void incrementMetricCounter(MetricType metric, StackView stack) {
        incrementMetricCounter(metric,
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
    }

    public void incrementMetricCounter(MetricType metric, StackView stack, Exception e) {
        incrementMetricCounter(metric,
                MetricTag.EXCEPTION_TYPE.name(), e.getClass().getName(),
                CloudbreakMetricTag.STACK_TYPE.name(), stack.getType().name(),
                MetricTag.CLOUD_PROVIDER.name(), stack.cloudPlatform(),
                MetricTag.TUNNEL_TYPE.name(), stack.getTunnel().name());
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}
