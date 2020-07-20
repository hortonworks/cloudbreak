package com.sequenceiq.redbeams.metrics;

import java.util.Optional;

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

    public void incrementMetricCounter(MetricType metricType, Optional<DBStack> dbStack) {
        String dbVendorName = dbStack
                .filter(db -> db.getDatabaseServer() != null)
                .map(db -> db.getDatabaseServer().getDatabaseVendor().displayName())
                .orElse("UNKNOWN");
        incrementMetricCounter(metricType,
                RedbeamsMetricTag.DATABASE_VENDOR.name(), dbVendorName);
    }
}