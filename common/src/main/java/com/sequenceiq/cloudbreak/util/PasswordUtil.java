package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.RandomStringUtils;

public class PasswordUtil {

    private static final int PWD_PREFIX_LENGTH = 6;

    private static final int PWD_PART_LENGTH = 10;

    private static final String SPECIAL_CHARS = ".-!~^";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generatePassword() {
        String prefix = PasswordUtil.getRandomAlphabetic(PWD_PREFIX_LENGTH);
        String letters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH);
        String numbers = PasswordUtil.getRandomNumeric(PWD_PART_LENGTH);
        String raw = letters.concat(numbers);
        List<String> list = Arrays.asList(raw.split(""));
        Collections.shuffle(list, SECURE_RANDOM);
        return prefix.concat(String.join("", list));
    }

    public static String generateCmAndPostgresConformPassword() {
        String prefix = PasswordUtil.getRandomAlphabetic(PWD_PREFIX_LENGTH);
        String letters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH);
        String numbers = PasswordUtil.getRandomNumeric(PWD_PART_LENGTH);
        String specials = getRandomSpecial(PWD_PART_LENGTH);
        String raw = letters.concat(numbers).concat(specials);
        List<String> list = Arrays.asList(raw.split(""));
        Collections.shuffle(list, SECURE_RANDOM);
        return prefix.concat(String.join("", list));
    }

    public static String getRandomSpecial(int count) {
        return RandomStringUtils.random(count, 0, 0, false, false, SPECIAL_CHARS.toCharArray(), SECURE_RANDOM);
    }

    public static String getRandomAlphabetic(int count) {
        return generate(count, true, false);
    }

    public static String getRandomAlphabeticWithLowerCaseOnly(int count) {
        return getRandomAlphabetic(count).toLowerCase(Locale.ROOT);
    }

    public static String getRandomNumeric(int count) {
        return generate(count, false, true);
    }

    public static String generate(int count, boolean letters, boolean numbers) {
        return RandomStringUtils.random(count, 0, 0, letters, numbers, null, SECURE_RANDOM);
    }

}
