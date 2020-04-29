package com.sequenceiq.cloudbreak.audit.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public class TestUtil {

    private TestUtil() {
    }

    /**
     * Asserts whether the constructor can be called, e.g. an instance can be built
     * @param constructor a {@link ThrowingCallable} constructor
     * @param valid whether the construction is valid. When it is, any exception is a failure. If invalid, the expected error message is asserted.
     * @param expectedThrowable the {@link Throwable} expected if conrstruction should be invalid
     * @param expectedErrorMessage the error message expected if conrstruction should be invalid
     */
    public static void assertConstruction(ThrowingCallable constructor, boolean valid, Class<?> expectedThrowable,  String expectedErrorMessage) {
        if (valid) {
            if (expectedThrowable != null || expectedErrorMessage != null) {
                throw new IllegalStateException("If construction is valid, expected items should be null.");
            }
            try {
                constructor.call();
            } catch (Throwable t) {
                throw new IllegalStateException("Constructor call should not throw any errors.", t);
            }
        } else {
            if (expectedThrowable == null) {
                throw new IllegalStateException("If construction is invalid, expected throwable should not be null.");
            }
            assertThatThrownBy(constructor).isInstanceOf(expectedThrowable).hasMessage(expectedErrorMessage);
        }
    }
}
