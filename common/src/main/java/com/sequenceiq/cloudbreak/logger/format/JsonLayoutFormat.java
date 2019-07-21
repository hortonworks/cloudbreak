package com.sequenceiq.cloudbreak.logger.format;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class JsonLayoutFormat extends SimpleLayoutFormat {

    private final CustomJsonLayout jsonLayout;

    JsonLayoutFormat(String prefix) {
        this.jsonLayout = new CustomJsonLayout(prefix);
        jsonLayout.setIncludeMessage(true);
        jsonLayout.setAppendLineSeparator(true);
        jsonLayout.setJsonFormatter(m -> new ObjectMapper().writeValueAsString(m));
    }

    @Override
    public String format(ILoggingEvent event, String message, String loggerNameFilter) {
        String filteredLogMessage = super.format(event, message, loggerNameFilter);
        return jsonLayout.doLayout(event, filteredLogMessage, loggerNameFilter);
    }
}
