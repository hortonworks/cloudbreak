package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class EventLoggerFactory {

    private EventLoggerFactory() {

    }

    public static void buildMdcContext(CloudbreakEvent cloudbreakEvent) {
        if (cloudbreakEvent.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cloudbreakEvent.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.EVENT.toString());
        if (cloudbreakEvent.getEventType() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), cloudbreakEvent.getEventType());
        }
        if (cloudbreakEvent.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cloudbreakEvent.getId().toString());
        }
    }
}
