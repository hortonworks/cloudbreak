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
    SDX_BACKUP_VALIDATION_REQUESTED("sdx.backup.validation.requested"),
    SDX_BACKUP_VALIDATION_FINISHED("sdx.backup.validation.finished"),
    SDX_BACKUP_VALIDATION_FAILED("sdx.backup.validation.failed"),
    SDX_BACKUP_FINISHED("sdx.backup.finished"),
    SDX_RESTORE_REQUESTED("sdx.restore.requested"),
    SDX_RESTORE_FAILED("sdx.restore.failed"),
    SDX_RESTORE_FINISHED("sdx.restore.finished"),
    SDX_RESTORE_VALIDATION_REQUESTED("sdx.restore.validation.requested"),
    SDX_RESTORE_VALIDATION_FINISHED("sdx.restore.validation.finished"),
    SDX_RESTORE_VALIDATION_FAILED("sdx.restore.validation.failed"),
    UPGRADE_CCM_FINISHED("sdx.upgrade.ccm.finished"),
    UPGRADE_CCM_FAILED("sdx.upgrade.ccm.failed"),
    ROTATE_DATABASE_CERTIFICATE_FINISHED("sdx.rotate.database.certificate.finished"),
    ROTATE_DATABASE_CERTIFICATE_FAILED("sdx.rotate.database.certificate.failed"),
    UPGRADE_DATABASE_SERVER_FINISHED("sdx.upgrade.database.finished"),
    UPGRADE_DATABASE_SERVER_FAILED("sdx.upgrade.database.failed"),
    IMD_UPDATE_FINISHED("sdx.update.imd.finished"),
    IMD_UPDATE_FAILED("sdx.update.imd.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
