package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class StackLoggerFactory {

    private StackLoggerFactory() {

    }

    public static void buildMdcContext(Stack stack) {
        if (stack.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), stack.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.STACK.toString());
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), stack.getName());
        if (stack.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stack.getId().toString());
        }
    }
}
