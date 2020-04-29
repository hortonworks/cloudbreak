package com.sequenceiq.cloudbreak.util;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class UuidUtil {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

    private UuidUtil() {
    }

    public static boolean isValid(String uuid) {
        if (uuid == null) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid).matches();
    }

    public static Supplier<String> uuidSupplier() {
        return () -> UUID.randomUUID().toString();
    }
}
