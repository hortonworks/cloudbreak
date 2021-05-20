package com.sequenceiq.cloudbreak.logger.format;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.logger.PartialMessageMetadata;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.CoreConstants;

public class CustomJsonLayout extends JsonLayout {

    private static final String PARTIAL_MESSAGE_FIELD = "partial_message";

    private static final String PARTIAL_CHUNK_ID_FIELD = "partial_id";

    private static final String PARTIAL_CHUNK_ID_PREFIX = "p";

    private static final String PARTIAL_CHUNK_INDEX_FIELD = "partial_ordinal";

    private static final String PARTIAL_LAST_FIELD = "partial_last";

    private static final String PARTIAL_MESSAGE_FLAG = "true";

    private final String contextName;

    private final Integer maxChunkLength;

    private final Splitter splitter;

    CustomJsonLayout(String contextName, Integer maxChunkLength) {
        this.contextName = contextName;
        this.maxChunkLength = maxChunkLength;
        this.splitter = Splitter.fixedLength(this.maxChunkLength);
    }

    public String doLayout(ILoggingEvent event, String fullLogMessage) {
        if (StringUtils.length(fullLogMessage) > maxChunkLength) {
            StringBuilder stringBuilder = new StringBuilder();
            String chunkId = PARTIAL_CHUNK_ID_PREFIX + Integer.toHexString(event.hashCode());
            Iterable<String> messages = splitter.split(fullLogMessage);
            Iterator<String> messageIterator = messages.iterator();
            for (int currentIndex = 0; messageIterator.hasNext(); currentIndex++) {
                String message = messageIterator.next();
                PartialMessageMetadata partialMessageMetadata = new PartialMessageMetadata(chunkId, currentIndex, !messageIterator.hasNext());
                stringBuilder.append(toJsonRecordString(toJsonMap(event, message, partialMessageMetadata)));
            }
            return stringBuilder.toString();
        } else {
            return toJsonRecordString(toJsonMap(event, fullLogMessage, null));
        }
    }

    private String toJsonRecordString(Map<String, Object> map) {
        if (map.isEmpty()) {
            return null;
        }
        return toJsonString(map) + CoreConstants.LINE_SEPARATOR;
    }

    private Map<String, Object> toJsonMap(ILoggingEvent event, String fullLogMessage, PartialMessageMetadata partialMessageMetadata) {
        Map<String, Object> map = new LinkedHashMap<>();
        addTimestamp(TIMESTAMP_ATTR_NAME, this.includeTimestamp, event.getTimeStamp(), map);
        add(LEVEL_ATTR_NAME, this.includeLevel, String.valueOf(event.getLevel()), map);
        add(THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
        add(LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            map.put(contextName, event.getMDCPropertyMap());
        }
        if (partialMessageMetadata != null) {
            add(PARTIAL_CHUNK_ID_FIELD, true, partialMessageMetadata.getPartialId(), map);
            add(PARTIAL_CHUNK_INDEX_FIELD, true, partialMessageMetadata.getPartialOrdinal(), map);
            add(PARTIAL_MESSAGE_FIELD, true, PARTIAL_MESSAGE_FLAG, map);
            add(PARTIAL_LAST_FIELD, true, partialMessageMetadata.getPartialLast(), map);
        }
        add(FORMATTED_MESSAGE_ATTR_NAME, true, fullLogMessage, map);
        return map;
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return getJsonFormatter().toJsonString(map);
        } catch (Exception e) {
            addError("JsonFormatter failed.  Defaulting to map.toString().  Message: " + e.getMessage(), e);
            return map.toString();
        }
    }
}
