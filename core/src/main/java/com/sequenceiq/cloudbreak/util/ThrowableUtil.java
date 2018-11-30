package com.sequenceiq.cloudbreak.util;

public class ThrowableUtil {

    private ThrowableUtil() {
    }

    public static <T> T getSpecificCauseRecursively(Throwable t, Class<T> causeType) {
        if (causeType.isInstance(t)) {
            return causeType.cast(t);
        } else if (t == null) {
            return null;
        }
        return getSpecificCauseRecursively(t.getCause(), causeType);
    }
}
