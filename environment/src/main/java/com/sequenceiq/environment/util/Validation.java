package com.sequenceiq.environment.util;

public class Validation {

    private Validation() {
    }

    public static void notNull(Object o, String fieldName) {
        if (o == null) {
            throw new IllegalArgumentException(fieldName + " should not be null!");
        }
    }

}
