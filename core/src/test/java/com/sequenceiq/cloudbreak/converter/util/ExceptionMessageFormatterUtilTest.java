package com.sequenceiq.cloudbreak.converter.util;


import jakarta.ws.rs.ForbiddenException;

import org.junit.Assert;
import org.junit.Test;

public class ExceptionMessageFormatterUtilTest {

    @Test
    public void formatAccessDeniedMessage() {
        Long id = 1L;
        String resourceName = "resourceName";
        String expected = "Access to resourceName '1' is denied or resourceName doesn't exist.";

        try {
            ExceptionMessageFormatterUtil.formatAccessDeniedMessage(this::throwAccessDenied, resourceName, id);
        } catch (ForbiddenException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private void throwAccessDenied() {
        throw new ForbiddenException("");
    }
}