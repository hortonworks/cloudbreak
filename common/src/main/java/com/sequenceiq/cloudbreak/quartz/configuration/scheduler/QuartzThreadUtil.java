package com.sequenceiq.cloudbreak.quartz.configuration.scheduler;

import java.util.Locale;

public class QuartzThreadUtil {

    private QuartzThreadUtil() {
    }

    public static boolean isCurrentQuartzThread() {
        return Thread.currentThread().getName().toLowerCase(Locale.ROOT).contains("quartz");
    }
}
