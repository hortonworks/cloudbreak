package com.sequenceiq.it.cloudbreak.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;

public class StructuredEventUtil {

    public static final int MAX_EVENT_NUMBER = 100;

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventUtil.class);

    private StructuredEventUtil() {
    }

    public static List<CDPStructuredEvent> getAuditEvents(CDPStructuredEventV1Endpoint endpoint, String resourceCrn) {
        try {
            return endpoint.getAuditEvents(resourceCrn, List.of(), 0, MAX_EVENT_NUMBER);
        } catch (Exception e) {
            LOGGER.error("Failed to load structured events. {}", e.getMessage(), e);
            return List.of();
        }
    }
}