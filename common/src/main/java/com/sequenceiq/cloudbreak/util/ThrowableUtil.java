package com.sequenceiq.cloudbreak.util;

import java.util.function.Supplier;

import javax.ws.rs.NotFoundException;

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

    public static Supplier<NotFoundException> notFound(String what, Object which) {
        return () -> new NotFoundException(String.format("%s '%s' not found.", what, which));
    }
}
