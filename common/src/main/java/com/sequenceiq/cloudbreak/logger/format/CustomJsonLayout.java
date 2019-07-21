package com.sequenceiq.cloudbreak.logger.format;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import java.util.LinkedHashMap;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.CoreConstants;

public class CustomJsonLayout extends JsonLayout {

    private final String prefix;

    CustomJsonLayout(String prefix) {
        this.prefix = prefix;
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
        addTimestamp("@" + TIMESTAMP_ATTR_NAME, this.includeTimestamp, event.getTimeStamp(), map);
        add(LEVEL_ATTR_NAME, this.includeLevel, String.valueOf(event.getLevel()), map);
        add(THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
        add(LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
        addMdcMapWithPrefix(event.getMDCPropertyMap(), map, this.prefix);
        String exceptionKey = this.prefix + EXCEPTION_ATTR_NAME;
        addThrowableInfo(exceptionKey, this.includeException, event, map);
        String message = event.getFormattedMessage();
        if (map.containsKey(exceptionKey)) {
            message = String.format("%s%n%s", message, map.get(exceptionKey));
            map.remove(exceptionKey);
        }
        if (loggerNameFilter != null && event.getLoggerName().startsWith(loggerNameFilter)) {
            message = anonymize(message);
        }
        add(this.prefix + "log_message", true, message, map);
        add("@" + FORMATTED_MESSAGE_ATTR_NAME, this.includeFormattedMessage, fullLogMessage, map);
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

    private void addMdcMapWithPrefix(Map<String, ?> mdcPropertMap, Map<String, Object> map, String prefix) {
        if (!mdcPropertMap.isEmpty()) {
            for (Map.Entry<String, ?> mdcEntry : mdcPropertMap.entrySet()) {
                map.put(prefix + mdcEntry.getKey(), mdcEntry.getValue());
            }
        }
    }
}
