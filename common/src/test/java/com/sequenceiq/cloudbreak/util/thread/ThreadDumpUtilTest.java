package com.sequenceiq.cloudbreak.util.thread;

import java.lang.management.ThreadInfo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ThreadDumpUtilTest {

    @Test
    void testDump() {
        ThreadInfo[] dump = ThreadDumpUtil.dump();

        String currentThreadName = Thread.currentThread().getName();
        StringBuffer threadDump = new StringBuffer(System.lineSeparator());
        for (ThreadInfo threadInfo : ThreadDumpUtil.dump()) {
            threadDump.append(threadInfo.toString());
        }
        Assertions.assertThat(threadDump).contains(currentThreadName);
    }

}