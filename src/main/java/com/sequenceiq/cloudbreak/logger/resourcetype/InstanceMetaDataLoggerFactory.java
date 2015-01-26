package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class InstanceMetaDataLoggerFactory {

    private InstanceMetaDataLoggerFactory() {

    }

    public static void buildMdcContext(InstanceMetaData instance) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), "cloudbreak");
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.INSTANCE_META_DATA.toString());
        if (instance.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        }
        if (instance.getLongName() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), instance.getLongName());
        }
    }
}
