package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;

import org.apache.commons.lang3.RandomStringUtils;

public class PasswordUtil {

    private static final int PWD_LENGTH = 26;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generatePassword() {
        return getRandomLettersAndNumbers(PWD_LENGTH);
    }

    public static String getRandomLettersAndNumbers(int count) {
        return generate(count, true, true);
    }

    public static String getRandomAlphabetic(int count) {
        return generate(count, true, false);
    }

    public static String getRandomNumeric(int count) {
        return generate(count, false, true);
    }

    private static String generate(int count, boolean letters, boolean numbers) {
        return RandomStringUtils.random(count, 0, 0, letters, numbers, null, SECURE_RANDOM);
    }
}
