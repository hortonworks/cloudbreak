package com.sequenceiq.cloudbreak.converter.util;


import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;

public class ExceptionMessageFormatterUtilTest {

    @Test
    public void formatAccessDeniedMessage() {
        Long id = 1L;
        String resourceName = "resourceName";
        String expected = "Access to resourceName '1' is denied or resourceName doesn't exist.";

        try {
            ExceptionMessageFormatterUtil.formatAccessDeniedMessage(this::throwAccessDenied, resourceName, id);
        } catch (AccessDeniedException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private void throwAccessDenied() {
        throw new AccessDeniedException("");
    }
}