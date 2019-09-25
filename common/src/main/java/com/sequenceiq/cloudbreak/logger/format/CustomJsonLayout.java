package com.sequenceiq.cloudbreak.logger.format;

import java.util.LinkedHashMap;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.CoreConstants;

public class CustomJsonLayout extends JsonLayout {

    private final String contextName;

    CustomJsonLayout(String contextName) {
        this.contextName = contextName;
    }

    public String doLayout(ILoggingEvent event, String fullLogMessage, String loggerNameFilter) {
        Map map = toJsonMap(event, fullLogMessage, loggerNameFilter);
        if (map == null || map.isEmpty()) {
            return null;
        }
        return toJsonString(map) + CoreConstants.LINE_SEPARATOR;
    }

    private Map toJsonMap(ILoggingEvent event, String fullLogMessage, String loggerNameFilter) {
        Map<String, Object> map = new LinkedHashMap<>();
        addTimestamp(TIMESTAMP_ATTR_NAME, this.includeTimestamp, event.getTimeStamp(), map);
        add(LEVEL_ATTR_NAME, this.includeLevel, String.valueOf(event.getLevel()), map);
        add(THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
        add(LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            map.put(contextName, event.getMDCPropertyMap());
        }
        add(FORMATTED_MESSAGE_ATTR_NAME, true, fullLogMessage, map);
        return map;
    }

    private String toJsonString(Map map) {
        try {
            return getJsonFormatter().toJsonString(map);
        } catch (Exception e) {
            addError("JsonFormatter failed.  Defaulting to map.toString().  Message: " + e.getMessage(), e);
            return map.toString();
        }
    }
}
