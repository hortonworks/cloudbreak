package com.sequenceiq.cloudbreak.logger.format;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface LayoutFormat {
    String format(ILoggingEvent event, String message, String loggerNameFilter);
}
