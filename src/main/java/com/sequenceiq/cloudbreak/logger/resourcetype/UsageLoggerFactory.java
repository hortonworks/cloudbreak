package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class UsageLoggerFactory {

    private UsageLoggerFactory() {

    }

    public static void buildMdcContext(CloudbreakUsage cloudbreakUsage) {
        if (cloudbreakUsage.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cloudbreakUsage.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.USAGE.toString());
        if (cloudbreakUsage.getCloud() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), cloudbreakUsage.getCloud());
        }
        if (cloudbreakUsage.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cloudbreakUsage.getId().toString());
        }
    }
}
