package com.sequenceiq.sdx.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinuxDirectoryPathValidatorTest {

    private LinuxDirectoryPathValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LinuxDirectoryPathValidator();
    }

    @Test
    void validAbsolutePaths() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("/", null));
        assertTrue(validator.isValid("/home", null));
        assertTrue(validator.isValid("/var/log", null));
        assertTrue(validator.isValid("/opt/my-app/config", null));
        assertTrue(validator.isValid("/tmp/", null));
    }

    @Test
    void invalidRelativePaths() {
        assertFalse(validator.isValid("home/user", null));
        assertFalse(validator.isValid("var/log", null));
        assertFalse(validator.isValid("tmp", null));
        assertFalse(validator.isValid(".", null));
        assertFalse(validator.isValid("..", null));
    }

    @Test
    void invalidPaths() {
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid(" ", null));
        assertFalse(validator.isValid("//double-slash", null));
        assertFalse(validator.isValid("/path//double", null));
        assertFalse(validator.isValid("/path/with\0null", null));
        assertFalse(validator.isValid("C:\\windows\\style", null));
    }
}
