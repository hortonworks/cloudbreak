package com.sequenceiq.consumption.configuration.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;

@Service
public class ConsumptionMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "consumption";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }

}
