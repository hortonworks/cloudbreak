package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;

public class RandomUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomUtil() {
    }

    public static int getInt(int maximumNumber) {
        return RANDOM.nextInt(maximumNumber);
    }

    public static long getLong(long maximumNumber) {
        return RANDOM.nextLong(maximumNumber);
    }

    public static SecureRandom getRandom() {
        return RANDOM;
    }
}
