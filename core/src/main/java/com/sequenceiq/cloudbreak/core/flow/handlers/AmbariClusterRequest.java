package com.sequenceiq.cloudbreak.core.flow.handlers;

import com.sequenceiq.cloudbreak.core.flow.context.AmbariClusterContext;

public class AmbariClusterRequest {

    private AmbariClusterContext clusterContext;

    public AmbariClusterRequest(AmbariClusterContext clusterContext) {
        this.clusterContext = clusterContext;
    }

    public static String selector(Class clazz) {
        return clazz.getSimpleName().toUpperCase();
    }

    public AmbariClusterContext getClusterContext() {
        return clusterContext;
    }
}
