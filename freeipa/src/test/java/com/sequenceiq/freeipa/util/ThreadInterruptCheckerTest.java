package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class ThreadInterruptCheckerTest {

    @Test
    public void testThrowsTimeoutWhenInterrupted() {
        ThreadInterruptChecker underTest = new ThreadInterruptChecker();
        Thread.currentThread().interrupt();

        assertThrows(TimeoutException.class, underTest::throwTimeoutExIfInterrupted);

        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testDoNothingIfNoInterrupt() throws TimeoutException {
        new ThreadInterruptChecker().throwTimeoutExIfInterrupted();
    }
}