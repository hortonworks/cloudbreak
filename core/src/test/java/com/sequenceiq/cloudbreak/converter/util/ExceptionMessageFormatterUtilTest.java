package com.sequenceiq.cloudbreak.converter.util;


import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;

class ExceptionMessageFormatterUtilTest {

    @Test
    void formatAccessDeniedMessage() {
        Long id = 1L;
        String resourceName = "resourceName";
        String expected = "Access to resourceName '1' is denied or resourceName doesn't exist.";

        try {
            ExceptionMessageFormatterUtil.formatAccessDeniedMessage(this::throwAccessDenied, resourceName, id);
        } catch (ForbiddenException e) {
            assertEquals(expected, e.getMessage());
        }
    }

    private void throwAccessDenied() {
        throw new ForbiddenException("");
    }
}