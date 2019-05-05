package com.sequenceiq.cloudbreak.logger;


import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {

    private String loggerNameFilter;

    public String getLoggerNameFilter() {
        return loggerNameFilter;
    }

    public void setLoggerNameFilter(String loggerNameFilter) {
        if (loggerNameFilter != null) {
            this.loggerNameFilter = loggerNameFilter.trim();
        }
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        if (loggerNameFilter != null && event.getLoggerName().startsWith(loggerNameFilter)) {
            message = anonymize(message);
        }
        return message;
    }
}
