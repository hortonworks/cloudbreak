package com.sequenceiq.periscope.utils;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public final class ClusterUtils {

    private ClusterUtils() {
        throw new IllegalStateException();
    }

    public static double computeFreeResourceRate(ClusterMetricsInfo metrics) {
        return (double) metrics.getAvailableMB() / (double) metrics.getTotalMB();
    }
}
