package com.sequenceiq.consumption.configuration.metrics;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;

@Primary
@Service
public class ConsumptionMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "consumption";

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.of(METRIC_PREFIX);
    }

}
