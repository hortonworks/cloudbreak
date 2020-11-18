package com.sequenceiq.cloudbreak.logger;


import com.sequenceiq.cloudbreak.logger.format.LayoutFormat;
import com.sequenceiq.cloudbreak.logger.format.LayoutFormatFactory;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {

    private String loggerNameFilter;

    private final LayoutFormat layoutFormat = LayoutFormatFactory.getLayoutFormat();

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
        return layoutFormat.format(event, super.doLayout(event), loggerNameFilter);
    }
}
