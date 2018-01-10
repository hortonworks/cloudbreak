package com.sequenceiq.cloudbreak.util;

import java.util.UUID;

public final class NameUtil {

    private static final String DELIMITER = "_";

    private NameUtil() {

    }

    public static String postfixWithTimestamp(String in) {
        return in + DELIMITER + System.currentTimeMillis();
    }

    public static String cutTimestampPostfix(String in) {
        return in.replaceAll(DELIMITER + "([0-9]+)$", "");
    }

    public static String generateArchiveName(String name) {
        //generate new name for the archived image catalog to by pass unique constraint
        return new StringBuilder().append(name).append('_').append(UUID.randomUUID()).toString();
    }
}
