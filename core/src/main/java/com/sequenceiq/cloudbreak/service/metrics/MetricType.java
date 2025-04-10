package com.sequenceiq.cloudbreak.service.metrics;

import com.sequenceiq.cloudbreak.common.metrics.type.Metric;

public enum MetricType implements Metric {

    STACK_CREATION_SUCCESSFUL("stack.creation.successful"),
    STACK_CREATION_FAILED("stack.creation.failed"),
    STACK_UPSCALE_SUCCESSFUL("stack.upscale.successful"),
    STACK_UPSCALE_FAILED("stack.upscale.failed"),
    STACK_STOP_SUCCESSFUL("stack.stop.successful"),
    STACK_STOP_FAILED("stack.stop.failed"),
    STACK_START_SUCCESSFUL("stack.start.successful"),
    STACK_START_FAILED("stack.start.failed"),
    STACK_TERMINATION_SUCCESSFUL("stack.termination.successful"),
    STACK_TERMINATION_FAILED("stack.termination.failed"),
    STACK_RECOVERY_TEARDOWN_SUCCESSFUL("stack.recovery.teardown.successful"),
    STACK_RECOVERY_TEARDOWN_FAILED("stack.recovery.teardown.failed"),
    STACK_STATUS_CLOUDPLATFORM_COUNT("stack.status.cloudplatform.count"),
    STACK_STATUS_TUNNEL_COUNT("stack.status.tunnel.count"),

    STACK_IMAGE_COPY("stack.image.copy"),

    CLUSTER_CREATION_SUCCESSFUL("cluster.creation.successful"),
    CLUSTER_CREATION_FAILED("cluster.creation.failed"),
    CLUSTER_UPSCALE_SUCCESSFUL("cluster.upscale.successful"),
    CLUSTER_UPSCALE_FAILED("cluster.upscale.failed"),
    CLUSTER_STOP_SUCCESSFUL("cluster.stop.successful"),
    CLUSTER_STOP_FAILED("cluster.stop.failed"),
    CLUSTER_START_SUCCESSFUL("cluster.start.successful"),
    CLUSTER_START_FAILED("cluster.start.failed"),

    STACK_PREPARATION("stack.preparation.duration"),

    EXTERNAL_DATABASE_CREATION_SUCCESSFUL("externaldatabase.creation.successful"),
    EXTERNAL_DATABASE_CREATION_FAILED("externaldatabase.creation.failed"),
    EXTERNAL_DATABASE_TERMINATION_SUCCESSFUL("externaldatabase.termination.successful"),
    EXTERNAL_DATABASE_TERMINATION_FAILED("externaldatabase.termination.failed"),
    EXTERNAL_DATABASE_START_SUCCESSFUL("externaldatabase.start.successful"),
    EXTERNAL_DATABASE_START_FAILED("externaldatabase.start.failed"),
    EXTERNAL_DATABASE_STOP_SUCCESSFUL("externaldatabase.stop.successful"),
    EXTERNAL_DATABASE_STOP_FAILED("externaldatabase.stop.failed"),

    METERING_REPORT_SUCCESSFUL("metering.report.successful"),
    METERING_REPORT_FAILED("metering.report.failed"),
    AWS_VARIANT_MIGRATION_FAILED("aws.variant.migration.successful"),
    AWS_VARIANT_MIGRATION_SUCCESSFUL("aws.variant.migration.failed"),

    ROTATE_RDS_CERTIFICATE_SUCCESSFUL("externaldatabase.certificate.rotation.successful"),
    ROTATE_RDS_CERTIFICATE_FAILED("externaldatabase.certificate.rotation.failed"),

    MODIFY_SELINUX_SUCCESSFUL("stack.modify.selinux.successful"),
    MODIFY_SELINUX_FAILED("stack.modify.selinux.failed");

    private final String metricName;

    MetricType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String getMetricName() {
        return metricName;
    }
}
