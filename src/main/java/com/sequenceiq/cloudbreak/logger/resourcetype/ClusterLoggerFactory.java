package com.sequenceiq.cloudbreak.logger.resourcetype;

import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.LoggerResourceType;

public class ClusterLoggerFactory {

    private ClusterLoggerFactory() {

    }

    public static void buildMdcContext(Cluster cluster) {
        if (cluster.getOwner() != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.getOwner());
        }
        MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), LoggerResourceType.CLUSTER.toString());
        if (cluster.getName() != null) {
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), cluster.getName());
        }
        if (cluster.getId() == null) {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined");
        } else {
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), cluster.getId().toString());
        }
    }
}
