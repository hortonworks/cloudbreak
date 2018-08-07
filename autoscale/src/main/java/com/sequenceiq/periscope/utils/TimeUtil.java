package com.sequenceiq.periscope.utils;

public class TimeUtil {

    public static final long SECOND_TO_MILLISEC = 1000L;

    public static final long MINUTE_TO_SECOND = 60L;

    public static final long MIN_IN_MS = SECOND_TO_MILLISEC * MINUTE_TO_SECOND;

    private TimeUtil() {
    }

    public static long convertMinToMillisec(long min) {
        return min * MIN_IN_MS;
    }
}
