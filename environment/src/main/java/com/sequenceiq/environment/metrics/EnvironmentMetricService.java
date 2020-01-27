package com.sequenceiq.environment.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;

@Service
public class EnvironmentMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "environment";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }

    public void incrementMetricCounter(MetricType metricType, EnvironmentDto environment) {
        incrementMetricCounter(metricType,
                EnvironmentMetricTag.NETWORK_REGISTRATION_TYPE.name(), environment.getNetwork().getRegistrationType().name(),
                MetricTag.TUNNEL_TYPE.name(), environment.getExperimentalFeatures().getTunnel().name(),
                MetricTag.CLOUD_PROVIDER.name(), environment.getCloudPlatform());
    }

    public void incrementMetricCounter(MetricType metricType, EnvironmentDto environment, Exception exception) {
        incrementMetricCounter(metricType,
                EnvironmentMetricTag.NETWORK_REGISTRATION_TYPE.name(), environment.getNetwork().getRegistrationType().name(),
                MetricTag.TUNNEL_TYPE.name(), environment.getExperimentalFeatures().getTunnel().name(),
                MetricTag.EXCEPTION_TYPE.name(), exception.getClass().getName(),
                MetricTag.CLOUD_PROVIDER.name(), environment.getCloudPlatform());
    }

    public void incrementMetricCounter(MetricType metricType, Environment environment, Exception exception) {
        incrementMetricCounter(metricType,
                EnvironmentMetricTag.NETWORK_REGISTRATION_TYPE.name(), environment.getNetwork().getRegistrationType().name(),
                MetricTag.TUNNEL_TYPE.name(), environment.getExperimentalFeaturesJson().getTunnel().name(),
                MetricTag.EXCEPTION_TYPE.name(), exception.getClass().getName(),
                MetricTag.CLOUD_PROVIDER.name(), environment.getCloudPlatform());
    }
}
