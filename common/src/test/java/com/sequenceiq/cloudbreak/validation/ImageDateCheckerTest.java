package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageDateCheckerTest {

    private static final String TEST_IMAGE_DATE = "2020-06-15";

    private ImageDateChecker undertest = new ImageDateChecker(TEST_IMAGE_DATE);

    @Test
    void testIsImageDateValid() {
        boolean result = undertest.isImageDateValidOrNull("2020-07-15");
        assertTrue(result);
    }

    @Test
    void testIsImageDateValidSame() {
        boolean result = undertest.isImageDateValidOrNull(TEST_IMAGE_DATE);
        assertTrue(result);
    }

    @Test
    void testIsImageDateValidLess() {
        boolean result = undertest.isImageDateValidOrNull("2020-02-15");
        assertFalse(result);
    }

    @Test
    void testIsImageDateValidInvalidDate() {
        boolean result = undertest.isImageDateValidOrNull("abd-efg");
        assertTrue(result);
    }

    @Test
    void testIsImageDateValidNull() {
        boolean result = undertest.isImageDateValidOrNull(null);
        assertTrue(result);
    }
}