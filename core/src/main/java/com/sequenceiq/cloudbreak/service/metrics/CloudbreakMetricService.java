package com.sequenceiq.cloudbreak.service.metrics;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;

@Service("MetricService")
public class CloudbreakMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "cloudbreak";

    @PostConstruct
    public void init() {
        for (MetricType m : MetricType.values()) {
            String metricName = m.getMetricName();
            if (metricName.startsWith("stack") || metricName.startsWith("cluster")) {
                initMicrometerMetricCounter(m, CloudConstants.AWS);
                initMicrometerMetricCounter(m, CloudConstants.AZURE);
                initMicrometerMetricCounter(m, CloudConstants.GCP);
                initMicrometerMetricCounter(m, CloudConstants.OPENSTACK);
                initMicrometerMetricCounter(m, CloudConstants.YARN);
            }
        }
    }

    /**
     * Increment a counter based metric. If the stack is provided then the metric's name
     * will be extended with the cloud platform. If the stack is null the original metric name will be used.
     *
     * @param metric Metric name
     * @param stack  Stack, used to determine the cloud platform
     */
    public void incrementMetricCounter(MetricType metric, StackView stack) {
        if (stack != null && stack.getPlatformVariant() != null) {
            incrementMetricCounter(getMetricNameWithPlatform(metric, stack.cloudPlatform()));
        } else {
            incrementMetricCounter(metric);
        }
    }

    /**
     * Increment a counter based metric. If the stack is provided then the metric's name
     * will be extended with the cloud platform. If the stack is null the original metric name will be used.
     *
     * @param metric Metric name
     * @param stack  Stack, used to determine the cloud platform
     */
    public void incrementMetricCounter(MetricType metric, Stack stack) {
        if (stack != null && stack.getPlatformVariant() != null) {
            incrementMetricCounter(getMetricNameWithPlatform(metric, stack.cloudPlatform()));
        } else {
            incrementMetricCounter(metric);
        }
    }

    private void initMicrometerMetricCounter(MetricType metric, String cloudPlatform) {
        initMicrometerMetricCounter(getMetricNameWithPlatform(metric, cloudPlatform));
    }

    private String getMetricNameWithPlatform(MetricType metric, String cloudPlatform) {
        return String.format("%s.%s.%s", METRIC_PREFIX, metric.getMetricName(), cloudPlatform.toLowerCase());
    }

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}
