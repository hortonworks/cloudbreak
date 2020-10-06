package com.sequenceiq.cloudbreak.logger.format;

import com.sequenceiq.cloudbreak.logger.LoggerConfiguration;

public class LayoutFormatFactory {

    private LayoutFormatFactory() {
    }

    public static LayoutFormat getLayoutFormat() {
        if (LoggerConfiguration.isJsonFormatEnabled()) {
            return new JsonLayoutFormat(LoggerConfiguration.getJsonFormatMdcName(), LoggerConfiguration.getSplitterMaxChunLength());
        }
        return new SimpleLayoutFormat();
    }
}
