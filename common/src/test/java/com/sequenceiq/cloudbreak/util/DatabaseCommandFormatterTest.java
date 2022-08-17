package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseCommandFormatterTest {

    private DatabaseCommandFormatter underTest;

    @BeforeEach
    void setUp() {
        underTest = new DatabaseCommandFormatter();
    }

    @Test
    void testIfNullPassedThenItShouldHandleIt() {
        assertDoesNotThrow(() -> underTest.encapsulateContentForLikelinessQuery(null));
    }

    @Test
    void testIfContentPassedTheExpectedResultShouldComeBack() {
        assertTrue(Pattern.compile("\\%:(.*):\\%").matcher(underTest.encapsulateContentForLikelinessQuery("something")).matches());
    }

}