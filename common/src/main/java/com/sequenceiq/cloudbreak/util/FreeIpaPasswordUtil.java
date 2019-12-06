package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

public class FreeIpaPasswordUtil {

    private static final int PWD_PREFIX_LENGTH = 2;

    private static final int PWD_PART_LENGTH = 8;

    private static final String SPECIAL_CHARS = "?.-_+";

    private FreeIpaPasswordUtil() {
    }

    public static String generatePassword() {
        String upperCaseLetters = RandomStringUtils.randomAlphabetic(PWD_PART_LENGTH).toUpperCase();
        String lowerCaseLetters = RandomStringUtils.randomAlphabetic(PWD_PART_LENGTH).toLowerCase();
        String pwdPrefix = RandomStringUtils.randomAlphabetic(PWD_PREFIX_LENGTH);
        String numbers = RandomStringUtils.randomNumeric(PWD_PART_LENGTH);
        String raw = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(SPECIAL_CHARS);
        List<String> list = Arrays.asList(raw.split(""));
        Collections.shuffle(list);
        return pwdPrefix.concat(list.toString());
    }
}
