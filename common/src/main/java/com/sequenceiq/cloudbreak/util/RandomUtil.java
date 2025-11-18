package com.sequenceiq.cloudbreak.util;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

public class RandomUtil {

    private static final String L_64_X_128_MIX_RANDOM = "L64X128MixRandom";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final RandomGenerator QUICK_RANDOM = RandomGenerator.of(L_64_X_128_MIX_RANDOM);

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

    public static int getQuickRandomInt(int maximumNumber) {
        return QUICK_RANDOM.nextInt(maximumNumber);
    }
}
