package com.sequenceiq.periscope.monitor.evaluator;

public class ScalingConstants {

    public static final int DEFAULT_CLUSTER_MIN_SIZE = 2;

    public static final int DEFAULT_CLUSTER_MAX_SIZE = 400;

    public static final int DEFAULT_CLUSTER_COOLDOWN_MINS = 5;

    public static final int DEFAULT_HOSTGROUP_MIN_SIZE = 0;

    public static final int DEFAULT_MAX_SCALE_UP_STEP_SIZE = 100;

    public static final int DEFAULT_MAX_SCALE_DOWN_STEP_SIZE = 100;

    public static final int DEFAULT_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS = 2;

    public static final int DEFAULT_MAX_LOAD_BASED_AUTOSCALING_COOLDOWN_MINS = 180;

    private ScalingConstants() {
    }
}
