package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class ThrowableUtilTest {

    @Test
    void testNullInput() {
        assertNull(ThrowableUtil.getSpecificCauseRecursively(null, ConstraintViolationException.class), "For null input it shall return with null");
    }

    @Test
    void testNullCause() {
        RuntimeException t = new RuntimeException();
        assertNull(ThrowableUtil.getSpecificCauseRecursively(t, ConstraintViolationException.class), "If no cause found it should return with null");
    }

    @Test
    void testExceptionIsTheRootCause() {
        ConstraintViolationException t = new ConstraintViolationException(null, null, null);
        assertEquals(t, ThrowableUtil.getSpecificCauseRecursively(t, ConstraintViolationException.class),
                "It should return with the same object if the root cause is the exception itself");
    }

    @Test
    void testExpectedExceptionIsOneLevelDeep() {
        ConstraintViolationException t = new ConstraintViolationException(null, null, null);
        RuntimeException rt = new RuntimeException("RT", t);
        assertEquals(t, ThrowableUtil.getSpecificCauseRecursively(rt, ConstraintViolationException.class), "Should find the desired exception if cause s set");
    }
}
