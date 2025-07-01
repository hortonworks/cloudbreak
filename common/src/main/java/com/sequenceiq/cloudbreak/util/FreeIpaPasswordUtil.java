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

    private static final int MAX_ATTEMPTS = 1000;

    private FreeIpaPasswordUtil() {
    }

    public static String generatePassword() {
        String upperCaseLetters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH).toUpperCase(Locale.ROOT);
        String lowerCaseLetters = PasswordUtil.getRandomAlphabetic(PWD_PART_LENGTH).toLowerCase(Locale.ROOT);
        String pwdPrefix = doNotRepeatCharacters(PasswordUtil.getRandomAlphabetic(PWD_PREFIX_LENGTH));
        String numbers = PasswordUtil.getRandomNumeric(PWD_PART_LENGTH);
        String raw = upperCaseLetters.concat(lowerCaseLetters).concat(numbers).concat(SPECIAL_CHARS);
        List<String> list = Arrays.asList(raw.split(""));
        Collections.shuffle(list, SECURE_RANDOM);
        return pwdPrefix.concat(doNotRepeatCharacters(String.join("", list)));
    }

    public static String doNotRepeatCharacters(String random) {
        int attempts = 0;
        String randomToCheck = random;
        while (hasTripleRepeat(randomToCheck)) {
            if (attempts > MAX_ATTEMPTS) {
                throw new IllegalStateException("Cannot generate password without repeating characters");
            }
            List<String> list = Arrays.asList(randomToCheck.split(""));
            Collections.shuffle(list, SECURE_RANDOM);
            randomToCheck = String.join("", list);
            attempts++;
        }
        return randomToCheck;
    }

    private static boolean hasTripleRepeat(String random) {
        for (int i = 0; i < random.length() - 2; i++) {
            char c = random.charAt(i);
            if (c == random.charAt(i + 1) && c == random.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

}
