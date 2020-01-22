package com.sequenceiq.redbeams.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;

@Service
public class RedbeamsMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "redbeams";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }
}