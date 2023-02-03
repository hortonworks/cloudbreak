package com.sequenceiq.datalake.metric;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.datalake.entity.SdxCluster;

@Primary
@Service
public class SdxMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "sdx";

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.of(METRIC_PREFIX);
    }

    public void incrementMetricCounter(MetricType metricType, SdxCluster sdxCluster) {
        incrementMetricCounter(metricType,
                SdxMetricTag.CLUSTER_SHAPE.name(), sdxCluster.getClusterShape().name());
    }
}
