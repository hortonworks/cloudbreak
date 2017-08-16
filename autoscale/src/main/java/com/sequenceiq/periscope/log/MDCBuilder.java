package com.sequenceiq.periscope.log;

import org.slf4j.MDC;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;

public class MDCBuilder {
    private MDCBuilder() {
    }

    public static void buildMdcContext() {
        buildMdcContext(null);
    }

    public static void buildMdcContext(Cluster cluster) {
        if (cluster == null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), "periscope");
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "");
            MDC.put(LoggerContextKey.CB_STACK_ID.toString(), "");
        } else {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.getUser().getId());
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), String.valueOf(cluster.getId()));
            MDC.put(LoggerContextKey.CB_STACK_ID.toString(), String.valueOf(cluster.getStackId()));
        }
    }

    public static void buildUserMdcContext(PeriscopeUser user) {
        buildMdcContext(user, null, null);
    }

    public static void buildMdcContext(PeriscopeUser user, Long clusterId) {
        buildMdcContext(user, null, clusterId);
    }

    public static void buildMdcContext(PeriscopeUser user, Long stackId, Long clusterId) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), user != null ? user.getId() : "");
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), clusterId != null ? String.valueOf(clusterId) : "");
        MDC.put(LoggerContextKey.CB_STACK_ID.toString(), stackId != null ? String.valueOf(stackId) : "");
    }
}
