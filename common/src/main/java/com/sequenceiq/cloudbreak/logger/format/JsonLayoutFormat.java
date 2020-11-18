package com.sequenceiq.cloudbreak.logger.format;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonLayoutFormat extends SimpleLayoutFormat {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private final CustomJsonLayout jsonLayout;

    JsonLayoutFormat(String contextName, Integer maxChunkLength) {
        jsonLayout = new CustomJsonLayout(contextName, maxChunkLength);
        jsonLayout.setIncludeMessage(true);
        jsonLayout.setAppendLineSeparator(true);
        jsonLayout.setJsonFormatter(m -> new ObjectMapper().writeValueAsString(m));
        jsonLayout.setTimestampFormat(DATE_FORMAT);
    }

    @Override
    public String format(ILoggingEvent event, String message, String loggerNameFilter) {
        String filteredLogMessage = super.format(event, message, loggerNameFilter);
        return jsonLayout.doLayout(event, filteredLogMessage);
    }
}
