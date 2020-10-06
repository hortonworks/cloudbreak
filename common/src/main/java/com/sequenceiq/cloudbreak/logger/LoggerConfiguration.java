package com.sequenceiq.cloudbreak.logger;

import org.apache.commons.lang3.StringUtils;

public class LoggerConfiguration {

    private LoggerConfiguration() {
    }

    public static Boolean isJsonFormatEnabled() {
        return Boolean.parseBoolean(getProperty("logger.format.json.enabled", null));
    }

    public static String getJsonFormatMdcName() {
        return getProperty("logger.format.json.mdc.name", "context");
    }

    public static Integer getSplitterMaxChunLength() {
        return Integer.parseInt(getProperty("logger.appender.splitter.max.chunk.length", "12000"));
    }

    private static String getProperty(String property, String defaultValue) {
        String envProperty = property.toUpperCase().replace(".", "_");
        if (StringUtils.isNotEmpty(System.getenv(envProperty))) {
            return System.getenv(envProperty);
        } else if (StringUtils.isNotEmpty(System.getProperty(property))) {
            return System.getProperty(property);
        }
        return defaultValue;
    }
}
