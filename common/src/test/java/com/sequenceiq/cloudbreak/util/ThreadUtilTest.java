package com.sequenceiq.cloudbreak.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ThreadUtilTest {

    @Test
    void testDump() {
        String dump = ThreadUtil.dump();
        String currentThreadName = Thread.currentThread().getName();

        Assertions.assertThat(dump).contains(currentThreadName);
    }
}