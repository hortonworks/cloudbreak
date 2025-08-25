package com.sequenceiq.redbeams.metrics;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {
    DB_PROVISION_FINISHED("db.provision.finished"),
    DB_PROVISION_FAILED("db.provision.failed"),
    DB_TERMINATION_FINISHED("db.termination.finished"),
    DB_TERMINATION_FAILED("db.termination.failed"),
    DB_START_FINISHED("db.start.finished"),
    DB_START_FAILED("db.start.failed"),
    DB_STOP_FINISHED("db.stop.finished"),
    DB_STOP_FAILED("db.stop.failed"),
    DB_ROTATE_CERT_FINISHED("db.rotatecert.finished"),
    DB_ROTATE_CERT_FAILED("db.rotatecert.failed"),
    DB_SSL_MIGRATION_FINISHED("db.ssl.migration.finished"),
    DB_SSL_MIGRATION_FAILED("db.ssl.migration.failed"),
    DB_UPGRADE_FINISHED("db.upgrade.finished"),
    DB_UPGRADE_FAILED("db.upgrade.failed"),
    DB_VALIDATE_UPGRADE_FINISHED("db.validate.upgrade.finished"),
    DB_VALIDATE_UPGRADE_FAILED("db.validate.upgrade.failed"),
    DB_VALIDATE_UPGRADE_CLEANUP_FINISHED("db.validate.upgrade.cleanup.finished"),
    DB_VALIDATE_UPGRADE_CLEANUP_FAILED("db.validate.upgrade.cleanup.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}