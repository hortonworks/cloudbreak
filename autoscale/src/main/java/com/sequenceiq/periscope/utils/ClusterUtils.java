package com.sequenceiq.periscope.utils;

import java.text.DecimalFormat;

import com.sequenceiq.ambari.client.services.ServiceAndHostService;

public final class ClusterUtils {

    public static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");

    public static final int MAX_CAPACITY = 100;

    private ClusterUtils() {
    }

    public static int getTotalNodes(ServiceAndHostService ambariClient) {
        return ambariClient.getClusterHosts().size();
    }
}
