package com.sequenceiq.periscope.common;

public class MessageCode {

    public static final String UNSUPPORTED_AUTOSCALING_TYPE = "autoscale.unsupported.autoscaling.type";

    public static final String UNSUPPORTED_AUTOSCALING_HOSTGROUP = "autoscale.unsupported.hostgroups";

    public static final String AUTOSCALING_CONFIG_NOT_FOUND = "autoscale.config.not.found";

    public static final String AUTOSCALING_CONFIG_UPDATED = "autoscale.config.updated";

    public static final String AUTOSCALING_CLUSTER_LIMIT_EXCEEDED = "autoscale.cluster.limit.exceeded";

    public static final String AUTOSCALING_ENABLED = "autoscale.enabled";

    public static final String AUTOSCALING_DISABLED = "autoscale.disabled";

    public static final String AUTOSCALING_ENTITLEMENT_NOT_ENABLED = "autoscale.entitlement.not.enabled";

    public static final String AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED = "autoscale.stopstart.entitlement.not.enabled";

    public static final String AUTOSCALING_ACTIVITY_NOT_REQUIRED = "autoscale.activity.not.required";

    public static final String AUTOSCALING_ACTIVITY_SUCCESS = "autoscale.activity.success";

    public static final String AUTOSCALING_ACTIVITY_NODE_LIMIT_EXCEEDED = "autoscale.activity.nodelimitexceeded";

    public static final String AUTOSCALING_TRIGGER_FAILURE = "autoscale.trigger.failure";

    public static final String SCHEDULE_CONFIG_OVERLAPS = "autoscale.schedule.config.overlap";

    public static final String CLUSTER_EXISTS_FOR_CRN = "autoscale.cluster.exists.for.crn";

    public static final String VALIDATION_SINGLE_TYPE = "autoscale.validation.single.type";

    public static final String VALIDATION_LOAD_SINGLE_HOST_GROUP = "autoscale.validation.load.single.hostgroup";

    public static final String VALIDATION_LOAD_UNSUPPORTED_ADJUSTMENT = "autoscale.validation.load.unsupported.adjustment";

    public static final String VALIDATION_LOAD_HOST_GROUP_DUPLICATE_CONFIG = "autoscale.validation.load.duplicate.config";

    public static final String VALIDATION_TIME_STOP_START_UNSUPPORTED = "autoscale.validation.time.stopstart.not.allowed";

    public static final String CLUSTER_SCALING_FAILED = "autoscale.cluster.scaling.failed";

    public static final String CLUSTER_UPDATE_IN_PROGRESS = "autoscale.cluster.update.in.progress";

    public static final String VALIDATION_TIME_NEGATIVE_ADJUSTMENT_FOR_EXACT = "autoscale.validation.time.negative.adjustment";

    public static final String NOTIFICATION_HISTORY_UPDATE = "autoscale.notification.history.update";

    public static final String NOTIFICATION_AS_CONFIG_UPDATE = "autoscale.notification.config.update";

    private MessageCode() { }
}
