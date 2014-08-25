package com.sequenceiq.periscope.log;

public final class PeriscopeLoggerFactory {

    private PeriscopeLoggerFactory() {
        throw new IllegalStateException();
    }

    public static Logger getLogger(Class clazz) {
        return new Logger(clazz);
    }

}
