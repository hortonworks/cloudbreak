package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventData;

public class EventDataLoggerFactory {

    private EventDataLoggerFactory() {

    }

    public static void buildMdcContext(CloudbreakEventData cloudbreakEventData) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), "cloudbreak");
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.EVENT_DATA.toString());
        if (cloudbreakEventData.getEventType() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), cloudbreakEventData.getEventType());
        }
        if (cloudbreakEventData.getEntityId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cloudbreakEventData.getEntityId().toString());
        }
    }
}
