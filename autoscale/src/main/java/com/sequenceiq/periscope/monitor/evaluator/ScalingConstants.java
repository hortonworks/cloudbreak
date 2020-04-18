package com.sequenceiq.periscope.monitor.evaluator;

public class ScalingConstants {

    public static final int DEFAULT_CLUSTER_MIN_SIZE = 2;

    public static final int DEFAULT_CLUSTER_MAX_SIZE = 200;

    public static final int DEFAULT_CLUSTER_COOLDOWN_MINS = 5;

    public static final int DEFAULT_HOSTGROUP_MIN_SIZE = 0;

    public static final int DEFAULT_HOSTGROUP_MAX_SIZE = 200;

    public static final int DEFAULT_MAX_SCALE_UP_STEP_SIZE = 50;

    private ScalingConstants() {
    }
}
