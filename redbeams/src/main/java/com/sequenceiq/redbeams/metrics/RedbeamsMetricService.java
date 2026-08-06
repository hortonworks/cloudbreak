package com.sequenceiq.redbeams.metrics;

import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.metrics.AbstractMetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Primary
@Service
public class RedbeamsMetricService extends AbstractMetricService {

    private static final String METRIC_PREFIX = "redbeams";

    @Override
    protected Optional<String> getMetricPrefix() {
        return Optional.of(METRIC_PREFIX);
    }

    public void incrementMetricCounter(MetricType metricType, Optional<DBStack> dbStack) {
        incrementMetricCounter(metricType,
                RedbeamsMetricTag.DATABASE_VENDOR.name(), databaseVendorName(dbStack));
    }

    public void incrementMetricCounter(MetricType metricType, Optional<DBStack> dbStack, String cloudPlatform) {
        incrementMetricCounter(metricType,
                RedbeamsMetricTag.DATABASE_VENDOR.name(), databaseVendorName(dbStack),
                MetricTag.CLOUD_PROVIDER.name(), StringUtils.hasText(cloudPlatform) ? cloudPlatform : "UNKNOWN");
    }

    private String databaseVendorName(Optional<DBStack> dbStack) {
        return dbStack
                .filter(db -> db.getDatabaseServer() != null)
                .map(db -> db.getDatabaseServer().getDatabaseVendor().displayName())
                .orElse("UNKNOWN");
    }
}
