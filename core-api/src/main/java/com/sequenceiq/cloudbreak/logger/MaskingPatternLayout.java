package com.sequenceiq.cloudbreak.logger;

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
        return super.doLayout(event);
    }
}
