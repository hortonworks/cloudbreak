package com.sequenceiq.datalake.service.sdx.attach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class SdxDetachNameGeneratorTest {
    private static final String TEST_NAME = "test-sdx";

    private static final Pattern DETACHED_NAME_PATTERN = Pattern.compile(".*(-[0-9]+)$");

    @InjectMocks
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateDetachedClusterName() {
        String newName = sdxDetachNameGenerator.generateDetachedClusterName(TEST_NAME);
        Matcher matcher = DETACHED_NAME_PATTERN.matcher(newName);
        assertTrue(matcher.matches());
    }

    @Test
    void testGenerateOriginalNameFromDetached() {
        String newName = sdxDetachNameGenerator.generateOriginalNameFromDetached(TEST_NAME + "-2");
        assertEquals(newName, TEST_NAME);
    }

    @Test
    void testGenerateOriginalNameFromDetachedInvalidInputName() {
        String errorMessage = "";

        try {
            sdxDetachNameGenerator.generateOriginalNameFromDetached(TEST_NAME);
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
        }

        assertEquals(errorMessage, String.format(
                "Provided detached name '%s' for generateOriginalNameFromDetached does not have expected pattern: '%s'!",
                TEST_NAME, DETACHED_NAME_PATTERN.pattern()
        ));
    }
}
