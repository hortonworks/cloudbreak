package com.sequenceiq.cloudbreak.logger.format;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class SimpleLayoutFormat implements LayoutFormat {

    @Override
    public String format(ILoggingEvent event, String message, String loggerNameFilter) {
        if (loggerNameFilter != null && event.getLoggerName().startsWith(loggerNameFilter)) {
            message = anonymize(message);
        }
        return message;
    }
}
