package com.sequenceiq.periscope.utils;

import java.text.DecimalFormat;

import com.sequenceiq.ambari.client.AmbariClient;

public final class ClusterUtils {

    public static final DecimalFormat TIME_FORMAT = new DecimalFormat("##.##");
    public static final int MAX_CAPACITY = 100;
    public static final int MIN_IN_MS = 1000 * 60;

    private ClusterUtils() {
    }

    public static int getTotalNodes(AmbariClient ambariClient) {
        return ambariClient.getClusterHosts().size();
    }
}
