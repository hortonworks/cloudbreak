package com.sequenceiq.cloudbreak.util.thread;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDumpUtil.class);

    private ThreadDumpUtil() {
    }

    public static void logThreadDumpOfEveryThread() {
        for (ThreadInfo threadInfo : dump()) {
            LOGGER.debug("ThreadInfo: {}", ThreadDumpFormatter.fullStackTrace(threadInfo));
        }
    }

    public static ThreadInfo[] dump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.dumpAllThreads(true, true);
    }
}
