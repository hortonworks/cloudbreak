package com.sequenceiq.datalake.metric;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {
    CUSTOM_SDX_REQUESTED("custom.sdx.requested"),
    INTERNAL_SDX_REQUESTED("internal.sdx.requested"),
    EXTERNAL_SDX_REQUESTED("external.sdx.requested"),
    SDX_CREATION_FINISHED("sdx.creation.finished"),
    SDX_CREATION_FAILED("sdx.creation.failed"),
    SDX_START_FINISHED("sdx.start.finished"),
    SDX_START_FAILED("sdx.start.failed"),
    SDX_REPAIR_FINISHED("sdx.repair.finished"),
    SDX_REPAIR_FAILED("sdx.repair.failed"),
    SDX_DELETION_FINISHED("sdx.deletion.finished"),
    SDX_DELETION_FAILED("sdx.deletion.failed"),
    SDX_BACKUP_REQUESTED("sdx.backup.requested"),
    SDX_BACKUP_FAILED("sdx.backup.failed"),
    SDX_BACKUP_FINISHED("sdx.backup.finished"),
    SDX_RESTORE_REQUESTED("sdx.restore.requested"),
    SDX_RESTORE_FAILED("sdx.restore.failed"),
    SDX_RESTORE_FINISHED("sdx.restore.finished"),
    UPGRADE_CCM_FINISHED("sdx.upgrade.ccm.finished"),
    UPGRADE_CCM_FAILED("sdx.upgrade.ccm.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
