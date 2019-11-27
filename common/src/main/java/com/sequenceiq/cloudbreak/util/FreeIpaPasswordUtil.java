package com.sequenceiq.cloudbreak.util;

import org.apache.commons.lang3.RandomStringUtils;

public class FreeIpaPasswordUtil {

    private static final int PWD_PART_LENGTH = 8;

    private static final String SPECIAL_CHARS = "?!.@-*_+";

    private static final int UPPER_CASE_LETTERS_START = 65;

    private static final int UPPER_CASE_LETTERS_END = 90;

    private static final int LOWER_CASE_LETTERS_START = 97;

    private static final int LOWER_CASE_LETTERS_END = 122;

    private static final int PASSWORD_LENGTH = 32;

    private FreeIpaPasswordUtil() {
    }

    public static String generatePassword() {
        String upperCaseLetters = RandomStringUtils.random(PWD_PART_LENGTH, UPPER_CASE_LETTERS_START, UPPER_CASE_LETTERS_END, true, true);
        String lowerCaseLetters = RandomStringUtils.random(PWD_PART_LENGTH, LOWER_CASE_LETTERS_START, LOWER_CASE_LETTERS_END, true, true);
        String numbers = RandomStringUtils.randomNumeric(PWD_PART_LENGTH);
        String raw = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(SPECIAL_CHARS);
        return RandomStringUtils.random(PASSWORD_LENGTH, raw);
    }
}
