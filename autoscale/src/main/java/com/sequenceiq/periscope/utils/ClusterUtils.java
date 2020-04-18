package com.sequenceiq.periscope.utils;

import java.text.DecimalFormat;

public class ClusterUtils {

    public static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");

    public static final int MAX_CAPACITY = 100;

    private ClusterUtils() {
    }

    public static long getRemainingCooldownTime(long coolDownMs, long lastScalingActivity) {
        return lastScalingActivity == 0L ? 0L : coolDownMs - (System.currentTimeMillis() - lastScalingActivity);
    }
}
