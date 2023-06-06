package com.sequenceiq.cloudbreak.util.thread;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;

import java.lang.management.ThreadInfo;

import org.junit.jupiter.api.Test;

class ThreadDumpFormatterTest {

    @Test
    void testThatTheDumpIsNotCut() {
        for (ThreadInfo threadInfo : ThreadDumpUtil.dump()) {
            String threadDump = ThreadDumpFormatter.fullStackTrace(threadInfo);
            assertThat(threadDump, not(containsString("...")));
        }
    }
}