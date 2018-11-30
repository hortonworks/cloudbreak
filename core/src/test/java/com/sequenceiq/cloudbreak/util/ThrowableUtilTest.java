package com.sequenceiq.cloudbreak.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

public class ThrowableUtilTest {

    @Test
    public void testNullInput() {
        assertNull("For null input it shall return with null",
                ThrowableUtil.getSpecificCauseRecursively(null, ConstraintViolationException.class));
    }

    @Test
    public void testNullCause() {
        RuntimeException t = new RuntimeException();
        assertNull("If no cause found it should return with null",
                ThrowableUtil.getSpecificCauseRecursively(t, ConstraintViolationException.class));
    }

    @Test
    public void testExceptionIsTheRootCause() {
        ConstraintViolationException t = new ConstraintViolationException(null, null, null);
        assertEquals("It should return with the same object if the root cause is the exception itself", t,
                ThrowableUtil.getSpecificCauseRecursively(t, ConstraintViolationException.class));
    }

    @Test
    public void testExpectedExceptionIsOneLevelDeep() {
        ConstraintViolationException t = new ConstraintViolationException(null, null, null);
        RuntimeException rt = new RuntimeException("RT", t);
        assertEquals("Should find the desired exception if cause s set", t,
                ThrowableUtil.getSpecificCauseRecursively(rt, ConstraintViolationException.class));
    }
}
