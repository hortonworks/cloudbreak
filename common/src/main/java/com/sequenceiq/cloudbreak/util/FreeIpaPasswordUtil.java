package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class FreeIpaPasswordUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int PWD_PREFIX_LENGTH = 3;

    private static final int PWD_PART_LENGTH = 8;

    private static final String SPECIAL_CHARS = "?.-_+";

    private static final int MAX_ATTEMPTS = 100000;

    private static final int MIN_INPUT_LENGTH = 3;

    private FreeIpaPasswordUtil() {
    }

    public static String generatePassword() {
        int attempts = 0;
        String generatedPassword = generateRandomPassword();
        while (hasTripleRepeatingCharacters(generatedPassword)) {
            if (attempts > MAX_ATTEMPTS) {
                throw new IllegalStateException("Cannot generate password without 3 repeating characters");
            }
            generatedPassword = generateRandomPassword();
            attempts++;
        }
        return generatedPassword;
    }

    private static String generateRandomPassword() {
        String upperCaseLetters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH).toUpperCase(Locale.ROOT);
        String lowerCaseLetters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH).toLowerCase(Locale.ROOT);
        String pwdPrefix = PasswordUtil.getRandomAlphabetic(PWD_PREFIX_LENGTH);
        String numbers = PasswordUtil.getRandomNumeric(PWD_PART_LENGTH);
        String raw = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(SPECIAL_CHARS);
        List<String> list = Arrays.asList(raw.split(""));
        Collections.shuffle(list, SECURE_RANDOM);
        return pwdPrefix.concat(String.join("", list));
    }

    public static boolean hasTripleRepeatingCharacters(String input) {
        if (input == null || input.length() < MIN_INPUT_LENGTH) {
            return false;
        }
        for (int i = 0; i < input.length() - 2; i++) {
            char c = input.charAt(i);
            if (c == input.charAt(i + 1) && c == input.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

}
