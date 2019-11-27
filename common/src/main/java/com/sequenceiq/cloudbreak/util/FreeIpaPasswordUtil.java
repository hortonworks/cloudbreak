package com.sequenceiq.cloudbreak.util;

import org.apache.commons.lang3.RandomStringUtils;

public class FreeIpaPasswordUtil {

    private static final int PWD_PART_LENGTH = 8;

    private static final String SPECIAL_CHARS = "?!./-*_+";

    private FreeIpaPasswordUtil() {
    }

    public static String generatePassword() {
        String upperCaseLetters = RandomStringUtils.random(PWD_PART_LENGTH, 65, 90, true, true);
        String lowerCaseLetters = RandomStringUtils.random(PWD_PART_LENGTH, 97, 122, true, true);
        String numbers = RandomStringUtils.randomNumeric(PWD_PART_LENGTH);
        String raw = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(SPECIAL_CHARS);
        return RandomStringUtils.random(32, raw);
    }
}
