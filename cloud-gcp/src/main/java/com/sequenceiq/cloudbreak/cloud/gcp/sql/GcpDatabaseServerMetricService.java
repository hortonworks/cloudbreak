package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.api.services.monitoring.v3.model.TimeSeries;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpMonitoringFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;

@Service
public class GcpDatabaseServerMetricService extends GcpDatabaseServerBaseService {

    // Cloud SQL disk utilization: fraction (0..1) of provisioned disk currently used.
    static final String DISK_UTILIZATION_METRIC = "cloudsql.googleapis.com/database/disk/utilization";

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseServerMetricService.class);

    private static final Duration METRIC_LOOKBACK = Duration.ofHours(1);

    private static final String ALIGNMENT_PERIOD_SECONDS = "300s";

    private static final double HUNDRED_PERCENT = 100.0d;

    private static final long BYTES_IN_GB = 1024L * 1024L * 1024L;

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpMonitoringFactory gcpMonitoringFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public Optional<DatabaseServerStorageMetrics> getDatabaseServerStorageMetrics(AuthenticatedContext ac, DatabaseStack stack) {
        GcpDatabaseServerView databaseServerView = new GcpDatabaseServerView(stack.getDatabaseServer());
        String deploymentName = databaseServerView.getDbServerName();
        String projectId = gcpStackUtil.getProjectId(ac.getCloudCredential());
        try {
            SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(ac.getCloudCredential(), ac.getCloudCredential().getName());
            Optional<DatabaseInstance> databaseInstance = getDatabaseInstance(deploymentName, sqlAdmin, projectId);
            if (databaseInstance.isEmpty()) {
                LOGGER.debug("GCP database instance {} not found, skipping storage metric read", deploymentName);
                return Optional.empty();
            }
            Long allocatedBytes = toBytes(databaseInstance.get().getSettings().getDataDiskSizeGb());
            Optional<Double> utilization = getLatestDiskUtilization(ac, projectId, deploymentName);
            if (utilization.isEmpty()) {
                LOGGER.debug("No {} metric reported yet for GCP database server {}", DISK_UTILIZATION_METRIC, deploymentName);
                return Optional.empty();
            }
            double freeStoragePercentage = (1.0d - utilization.get()) * HUNDRED_PERCENT;
            Long freeBytes = allocatedBytes == null ? null : Math.round(allocatedBytes * (freeStoragePercentage / HUNDRED_PERCENT));
            return Optional.of(new DatabaseServerStorageMetrics(freeStoragePercentage, freeBytes, allocatedBytes));
        } catch (Exception e) {
            LOGGER.warn("Failed to read GCP storage metrics for database server {}: {}", deploymentName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<Double> getLatestDiskUtilization(AuthenticatedContext ac, String projectId, String deploymentName) throws IOException {
        Monitoring monitoring = gcpMonitoringFactory.buildMonitoring(ac.getCloudCredential(), ac.getCloudCredential().getName());
        if (monitoring == null) {
            return Optional.empty();
        }
        Instant end = Instant.now();
        Instant start = end.minus(METRIC_LOOKBACK);
        String filter = String.format("metric.type=\"%s\" AND resource.labels.database_id=\"%s:%s\"",
                DISK_UTILIZATION_METRIC, projectId, deploymentName);
        ListTimeSeriesResponse response = monitoring.projects().timeSeries()
                .list("projects/" + projectId)
                .setFilter(filter)
                .setIntervalStartTime(start.toString())
                .setIntervalEndTime(end.toString())
                .setAggregationPerSeriesAligner("ALIGN_MEAN")
                .setAggregationAlignmentPeriod(ALIGNMENT_PERIOD_SECONDS)
                .execute();
        List<TimeSeries> timeSeries = response.getTimeSeries();
        if (timeSeries == null || timeSeries.isEmpty()) {
            return Optional.empty();
        }
        return timeSeries.stream()
                .filter(series -> series.getPoints() != null)
                .flatMap(series -> series.getPoints().stream())
                .filter(point -> point.getValue() != null && point.getValue().getDoubleValue() != null)
                .max(Comparator.comparing(point -> point.getInterval().getEndTime()))
                .map(point -> point.getValue().getDoubleValue());
    }

    private Long toBytes(Long dataDiskSizeGb) {
        return dataDiskSizeGb == null ? null : dataDiskSizeGb * BYTES_IN_GB;
    }
}
