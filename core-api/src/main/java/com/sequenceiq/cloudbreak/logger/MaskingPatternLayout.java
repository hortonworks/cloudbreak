package com.sequenceiq.cloudbreak.logger;

import com.sequenceiq.cloudbreak.logger.format.LayoutFormat;
import com.sequenceiq.cloudbreak.logger.format.LayoutFormatFactory;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MaskingPatternLayout extends PatternLayout {

    private final LayoutFormat layoutFormat = LayoutFormatFactory.getLayoutFormat();

    @Override
    public String doLayout(ILoggingEvent event) {
        return layoutFormat.format(event, super.doLayout(event));
    }
}
