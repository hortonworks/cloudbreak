package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;

public class CloudBreakLoggerFactory {

    private CloudBreakLoggerFactory() {

    }

    public static void buildMdvContext() {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), "cloudbreak");
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), "cloudbreakLog");
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), "cb");
    }
}
