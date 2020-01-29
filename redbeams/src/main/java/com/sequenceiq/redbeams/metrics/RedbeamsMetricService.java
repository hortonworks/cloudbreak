package com.sequenceiq.redbeams.metrics;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Service
public class RedbeamsMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "redbeams";

    @Override
    protected String getMetricPrefix() {
        return METRIC_PREFIX;
    }

    public void incrementMetricCounter(MetricType metricType, DBStack dbStack) {
        String dbVendorName = dbStack != null ? dbStack.getDatabaseServer()
                != null ? dbStack.getDatabaseServer().getDatabaseVendor().displayName() : "UNKNOWN" : "UNKNOWN";
        incrementMetricCounter(metricType,
                RedbeamsMetricTag.DATABASE_VENDOR.name(), dbVendorName);
    }
}