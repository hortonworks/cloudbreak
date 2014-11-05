package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class TemplateLoggerFactory {

    private TemplateLoggerFactory() {

    }

    public static void buildMdvContext(Template template) {
        if (template.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), template.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.TEMPLATE.toString());
        if (template.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), template.getName().toString());
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), template.getId().toString());
        }
    }
}
