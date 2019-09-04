package com.sequenceiq.cloudbreak.workspace.exception;

import java.util.function.Supplier;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static Supplier<NotFoundException> notFound(String what, String which) {
        return () -> new NotFoundException(String.format("%s '%s' not found.", what, which));
    }

    public static Supplier<NotFoundException> notFound(String what, Long which) {
        return () -> new NotFoundException(String.format("%s '%d' not found.", what, which));
    }
}
