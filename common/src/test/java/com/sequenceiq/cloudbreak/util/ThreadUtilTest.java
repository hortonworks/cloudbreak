package com.sequenceiq.cloudbreak.util;

import java.lang.management.ThreadInfo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ThreadUtilTest {

    @Test
    void testDump() {
        ThreadInfo[] dump = ThreadUtil.dump();

        String currentThreadName = Thread.currentThread().getName();
        StringBuffer threadDump = new StringBuffer(System.lineSeparator());
        for (ThreadInfo threadInfo : ThreadUtil.dump()) {
            threadDump.append(threadInfo.toString());
        }
        Assertions.assertThat(threadDump).contains(currentThreadName);
    }
}