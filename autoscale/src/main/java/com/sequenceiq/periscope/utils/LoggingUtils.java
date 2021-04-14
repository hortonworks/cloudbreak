package com.sequenceiq.periscope.utils;

import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.periscope.domain.Cluster;

public class LoggingUtils {

    private LoggingUtils() {
    }

    public static void buildMdcContext(Cluster cluster) {
        if (cluster != null) {
            MdcContext.builder()
                    .resourceCrn(cluster.getStackCrn())
                    .resourceType("AutoscaleCluster")
                    .resourceName(cluster.getStackName())
                    .tenant(cluster.getClusterPertain().getTenant())
                    .userCrn(cluster.getClusterPertain().getUserCrn())
                    .buildMdc();
        }
    }

    public static void buildMdcContextWithName(String resourceName) {
        MdcContext.builder().resourceType("AutoscaleCluster")
                .resourceName(resourceName).buildMdc();
    }

    public static void buildMdcContextWithCrn(String resourceCrn) {
        MdcContext.builder().resourceType("AutoscaleCluster")
                .resourceCrn(resourceCrn).buildMdc();
    }
}
