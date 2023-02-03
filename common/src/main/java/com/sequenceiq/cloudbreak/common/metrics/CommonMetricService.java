package com.sequenceiq.cloudbreak.common.metrics;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service("CommonMetricService")
public class CommonMetricService extends AbstractMetricService {

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.empty();
    }
}
