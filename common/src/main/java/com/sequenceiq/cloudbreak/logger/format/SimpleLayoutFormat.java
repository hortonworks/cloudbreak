package com.sequenceiq.cloudbreak.logger.format;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class SimpleLayoutFormat implements LayoutFormat {

    @Override
    public String format(ILoggingEvent event, String message, String loggerNameFilter) {
        return message;
    }
}
