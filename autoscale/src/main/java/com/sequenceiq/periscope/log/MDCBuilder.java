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
        } else {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), cluster.getUser().getId());
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), String.valueOf(cluster.getId()));
            MDC.put(LoggerContextKey.CB_STACK_ID.toString(), String.valueOf(cluster.getStackId()));
        }
    }

    public static void buildMdcContext(PeriscopeUser user, Long clusterId) {
        buildUserMdcContext(user);
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), String.valueOf(clusterId));
    }

    public static void buildUserMdcContext(PeriscopeUser user) {
        if (user != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), user.getId());
        }
    }

}
