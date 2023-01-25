package com.sequenceiq.cloudbreak.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadUtil {

    private ThreadUtil() {
    }

    public static ThreadInfo[] dump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.dumpAllThreads(true, true);
    }
}
