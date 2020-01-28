package com.sequenceiq.datalake.metric;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.datalake.entity.SdxCluster;

@Service
public class SdxMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "sdx";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }

    public void incrementMetricCounter(MetricType metricType, SdxCluster sdxCluster) {
        incrementMetricCounter(metricType,
                SdxMetricTag.CLUSTER_SHAPE.name(), sdxCluster.getClusterShape().name());
    }
}
