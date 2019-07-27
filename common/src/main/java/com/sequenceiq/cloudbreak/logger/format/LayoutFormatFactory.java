package com.sequenceiq.cloudbreak.logger.format;

import org.apache.commons.lang3.StringUtils;

public class LayoutFormatFactory {

    private LayoutFormatFactory() {
    }

    public static LayoutFormat getLayoutFormat() {
        final String property = getProperty("logger.format.json.enabled", null);
        if (Boolean.parseBoolean(property)) {
            final String mdcContextName = getProperty(
                    "logger.format.json.mdc.name", "context");
            return new JsonLayoutFormat(mdcContextName);
        }
        return new SimpleLayoutFormat();
    }

    private static String getProperty(String property, String defaultValue) {
        String env = System.getenv(property.toUpperCase().replace(".", "_"));
        if (StringUtils.isNotEmpty(env)) {
            return System.getenv(env);
        } else if (StringUtils.isNotEmpty(System.getProperty(property))) {
            return System.getProperty(property);
        }
        return defaultValue;
    }
}
