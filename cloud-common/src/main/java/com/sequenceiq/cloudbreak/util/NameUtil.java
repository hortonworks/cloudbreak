package com.sequenceiq.cloudbreak.util;

public final class NameUtil {

    private static final String DELIMITER = "_";

    private NameUtil() {

    }

    public static String postfixWithTimestamp(String in) {
        return in + DELIMITER + System.currentTimeMillis();
    }
}
