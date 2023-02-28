package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CBAssertion {

    private CBAssertion() {
    }

    /**
     * Comparing the two given objects for equal state.
     *
     * @param actual   the actual object which should contain the same value(s) as the expected one.
     * @param expected the general expected value.
     * @throws TestFailException if the comparison failed
     */
    public static void assertEquals(Object actual, Object expected) {
        assertEquals(String.format("%s expected but got: %s", expected, actual), actual, expected);
    }

    /**
     * Comparing the two given objects for equal state. If fails, the given message will be presented as a reason / description.
     *
     * @param message the given message will appear as the reason message if the comparison fails.
     * @param actual the actual object which should contain the same value(s) as the expected one.
     * @param expected the general expected value.
     * @throws TestFailException if the comparison failed
     */
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
