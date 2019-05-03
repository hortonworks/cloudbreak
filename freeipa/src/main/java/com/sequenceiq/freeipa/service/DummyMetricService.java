package com.sequenceiq.freeipa.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.metric.Metric;
import com.sequenceiq.cloudbreak.service.metrics.MetricService;

@Service
public class DummyMetricService implements MetricService {
    @Override
    public void submit(Metric metric, double value) {

    }

    @Override
    public void submit(Metric metric, double value, Map<String, String> labels) {

    }

    @Override
    public void initMicrometerMetricCounter(Metric metric) {

    }

    @Override
    public void incrementMetricCounter(Metric metric) {

    }
}
