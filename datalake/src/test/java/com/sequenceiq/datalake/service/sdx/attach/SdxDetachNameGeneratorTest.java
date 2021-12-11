package com.sequenceiq.datalake.service.sdx.attach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class SdxDetachNameGeneratorTest {
    private static final String TEST_NAME = "test-sdx";

    @InjectMocks
    private SdxDetachNameGenerator sdxDetachNameGenerator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateDetachedClusterName() {
        String newName = sdxDetachNameGenerator.generateDetachedClusterName(TEST_NAME);
        Matcher matcher = SdxDetachNameGenerator.DETACHED_NAME_PATTERN.matcher(newName);
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
                TEST_NAME, SdxDetachNameGenerator.DETACHED_NAME_PATTERN.pattern()
        ));
    }
}
