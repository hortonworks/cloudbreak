package com.sequenceiq.periscope.common;

public class MessageCode {

    public static final String UNSUPPORTED_AUTOSCALING_TYPE = "autoscale.unsupported.autoscaling.type";

    public static final String UNSUPPORTED_AUTOSCALING_HOSTGROUP = "autoscale.unsupported.hostgroups";

    public static final String AUTOSCALING_CONFIG_NOT_FOUND = "autoscale.config.not.found";

    public static final String AUTOSCALING_ENTITLEMENT_NOT_ENABLED = "autoscale.entitlement.not.enabled";

    public static final String CLUSTER_PROXY_NOT_CONFIGURED = "autoscale.load.clusterproxy.not.enabled";

    public static final String LOAD_CONFIG_ALREADY_DEFINED = "autoscale.load.config.already.defined";

    public static final String CLUSTER_EXISTS_FOR_CRN = "autoscale.cluster.exists.for.crn";

    public static final String VALIDATION_SINGLE_TYPE = "autoscale.validation.single.type";

    public static final String VALIDATION_LOAD_SINGLE_HOST_GROUP = "autoscale.validation.load.single.hostgroup";

    public static final String VALIDATION_LOAD_UNSUPPORTED_ADJUSTMENT = "autoscale.validation.load.unsupported.adjustment";

    public static final String VALIDATION_LOAD_HOST_GROUP_DUPLICATE_CONFIG = "autoscale.validation.load.duplicate.config";

    private MessageCode() { }
}
