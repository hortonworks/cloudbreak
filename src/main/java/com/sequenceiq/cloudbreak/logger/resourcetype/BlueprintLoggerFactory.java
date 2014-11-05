package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class BlueprintLoggerFactory {

    private BlueprintLoggerFactory() {

    }

    public static void buildMdcContext(Blueprint blueprint) {
        if (blueprint.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), blueprint.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.BLUEPRINT.toString());
        if (blueprint.getName() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), blueprint.getName());
        }
        if (blueprint.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), blueprint.getId().toString());
        }
    }
}
