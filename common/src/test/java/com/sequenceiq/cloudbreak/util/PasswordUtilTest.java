package com.sequenceiq.cloudbreak.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PasswordUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordUtilTest.class);

    @Test
    void testGeneratePassword() {
        long startInMillis = System.currentTimeMillis();
        String actual = PasswordUtil.generatePassword();
        long endInMillis = System.currentTimeMillis();
        LOGGER.info("Password has been generated within {}ms the generated password: '{}'", endInMillis - startInMillis, actual);
        Assertions.assertEquals(26, actual.length());
        Assertions.assertTrue(actual.chars().allMatch(charAsInt -> Character.isDigit(charAsInt)
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
        Assertions.assertTrue(actual.chars().allMatch(charAsInt -> Character.isUpperCase(charAsInt)
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
        Assertions.assertTrue(actual.chars().allMatch(Character::isDigit));
    }
}