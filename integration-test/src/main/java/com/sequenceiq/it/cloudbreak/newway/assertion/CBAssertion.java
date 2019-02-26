package com.sequenceiq.it.cloudbreak.newway.assertion;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CBAssertion {

    private CBAssertion() {
    }

    public static void assertEquals(Object actual, Object expected) {
        assertEquals(String.format("%s expected but got: %s", expected, actual), actual, expected);
    }

    public static void assertEquals(String message, Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw getException(message);
        }
    }

    public static void assertTrue(boolean exected) {
        assertTrue("Expected true, but got false", exected);
    }

    public static void assertTrue(String message, boolean exected) {
        if (!exected) {
            throw getException(message);
        }
    }

    private static TestFailException getException(String message) {
        return new TestFailException(message);
    }
}
