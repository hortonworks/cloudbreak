package com.sequenceiq.periscope.common;

public class MessageCode {

    public static final String UNSUPPORTED_AUTOSCALING_TYPE = "autoscale.unsupported.autoscaling.type";

    public static final String UNSUPPORTED_AUTOSCALING_HOSTGROUP = "autoscale.unsupported.hostgroups";

    public static final String AUTOSCALING_CONFIG_NOT_FOUND = "autoscale.config.not.found";

    public static final String AUTOSCALING_CONFIG_UPDATED = "autoscale.config.updated";

    public static final String AUTOSCALING_STOPPED_NODES_DELETION = "autoscale.stopped.nodes.deleted";

    public static final String AUTOSCALING_STOPPED_NODES_DELETION_FAILED = "autoscale.stopped.nodes.deleted.failed";

    public static final String AUTOSCALING_CLUSTER_LIMIT_EXCEEDED = "autoscale.cluster.limit.exceeded";

    public static final String AUTOSCALING_ENABLED = "autoscale.enabled";

    public static final String AUTOSCALING_DISABLED = "autoscale.disabled";

    public static final String AUTOSCALING_POLICIES_DELETED = "autoscale.alerts.deleted";

    public static final String AUTOSCALING_ENTITLEMENT_NOT_ENABLED = "autoscale.entitlement.not.enabled";

    public static final String AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED = "autoscale.stopstart.entitlement.not.enabled";

    public static final String IMPALA_SCHEDULE_BASED_SCALING_ENTITLEMENT_NOT_ENABLED = "impala.schedule.based.scaling.entitlement.not.enabled";

    public static final String AUTOSCALING_ACTIVITY_NOT_REQUIRED = "autoscale.activity.not.required";

    public static final String AUTOSCALING_TRIGGER_FOR_MIN_NODE = "autoscaling.trigger.for.min.node";

    public static final String AUTOSCALING_ACTIVITY_SUCCESS = "autoscale.activity.success";

    public static final String AUTOSCALING_ACTIVITY_NODE_LIMIT_EXCEEDED = "autoscale.activity.nodelimitexceeded";

    public static final String AUTOSCALE_STOPSTART_INITIAL_UPSCALE = "autoscale.stopstart.initial.upscale.adjustment";

    public static final String AUTOSCALING_TRIGGER_FAILURE = "autoscale.trigger.failure";

    public static final String AUTOSCALE_YARN_RECOMMENDATION_SUCCESS = "autoscale.yarn.recommendation.success";

    public static final String AUTOSCALE_YARN_RECOMMENDATION_FAILED = "autoscale.yarn.recommendation.failure";

    public static final String AUTOSCALE_UPSCALE_TRIGGER_SUCCESS = "autoscale.upscale.trigger.success";

    public static final String AUTOSCALE_DOWNSCALE_TRIGGER_SUCCESS_NODE_LIST = "autoscale.downscale.trigger.success.node.list";

    public static final String AUTOSCALE_DOWNSCALE_TRIGGER_SUCCESS = "autoscale.downscale.trigger.success";

    public static final String AUTOSCALE_UPSCALE_TRIGGER_FAILURE = "autoscale.upscale.trigger.failure";

    public static final String AUTOSCALE_DOWNSCALE_TRIGGER_FAILURE = "autoscale.downscale.trigger.failure";

    public static final String AUTOSCALE_SCHEDULE_BASED_UPSCALE = "autoscale.time.based.upscale";

    public static final String AUTOSCALE_SCHEDULE_BASED_DOWNSCALE = "autoscale.time.based.downscale";

    public static final String AUTOSCALE_MANDATORY_UPSCALE = "autoscale.mandatory.upscale";

    public static final String AUTOSCALE_MANDATORY_DOWNSCALE = "autoscale.mandatory.downscale";

    public static final String AUTOSCALE_SCALING_FLOW_FAILED = "autoscale.scaling.flow.failure";

    public static final String AUTOSCALE_SCALING_ACTIVITY_UNKNOWN = "autoscale.scaling.activity.unknown";

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
