package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PasswordUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordUtilTest.class);

    private static final int MIN_CM_PASSWORD_LENGTH = 32;

    @Test
    void testGeneratePassword() {
        long startInMillis = System.currentTimeMillis();
        String actual = PasswordUtil.generatePassword();
        long endInMillis = System.currentTimeMillis();
        LOGGER.info("Password has been generated within {}ms the generated password: '{}'", endInMillis - startInMillis, actual);
        Assertions.assertEquals(26, actual.length());
        assertTrue(actual.chars().allMatch(charAsInt -> Character.isDigit(charAsInt)
                || Character.isUpperCase(charAsInt)
                || Character.isLowerCase(charAsInt)));
    }

    @Test
    void testGenerateRandomAlphabetic() {
        long startInMillis = System.currentTimeMillis();
        int expectedCount = 12;
        String actual = PasswordUtil.getRandomAlphabetic(expectedCount);
        long endInMillis = System.currentTimeMillis();
        LOGGER.info("Random string with only alphabetic chars has been generated within {}ms the generated password: '{}'", endInMillis - startInMillis,
                actual);
        Assertions.assertEquals(expectedCount, actual.length());
        assertTrue(actual.chars().allMatch(charAsInt -> Character.isUpperCase(charAsInt)
                || Character.isLowerCase(charAsInt)));
    }

    @Test
    void testGenerateRandomNumeric() {
        long startInMillis = System.currentTimeMillis();
        int expectedCount = 16;
        String actual = PasswordUtil.getRandomNumeric(expectedCount);
        long endInMillis = System.currentTimeMillis();
        LOGGER.info("Random string with only numeric chars has been generated within {}ms the generated password: '{}'", endInMillis - startInMillis,
                actual);
        Assertions.assertEquals(expectedCount, actual.length());
        assertTrue(actual.chars().allMatch(Character::isDigit));
    }

    /**
     * Base on DefaultPasswordQualityFunction in starship repo
     */
    @Test
    void testCmAndPostgresConformPassword() {
        long startInMillis = System.currentTimeMillis();
        String actual = PasswordUtil.generateCmAndPostgresConformPassword();
        long endInMillis = System.currentTimeMillis();
        LOGGER.info("CM conform pass generated within {}ms the generated password: '{}'", endInMillis - startInMillis, actual);
        assertTrue(actual.length() >= MIN_CM_PASSWORD_LENGTH);
        assertTrue(actual.matches(".*[A-Z].*"));
        assertTrue(actual.matches(".*[a-z].*"));
        assertTrue(actual.matches(".*\\d.*"));
        assertTrue(actual.matches(".*[^a-zA-Z0-9].*"));
    }
}