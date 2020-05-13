package com.sequenceiq.periscope.utils;

import java.text.DecimalFormat;

public class ClusterUtils {

    public static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");

    public static final int MAX_CAPACITY = 100;

    public static final long  MEGABYTE = 1024L * 1024L;

    private ClusterUtils() {
    }

    public static long getRemainingCooldownTime(long coolDownMs, long lastScalingActivity) {
        return lastScalingActivity == 0L ? 0L : coolDownMs - (System.currentTimeMillis() - lastScalingActivity);
    }

    public static long memoryBytesToMB(long memoryInBytes) {
        return memoryInBytes / MEGABYTE;
    }
}
