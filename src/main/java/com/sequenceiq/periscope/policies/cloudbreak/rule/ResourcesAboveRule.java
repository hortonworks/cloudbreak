package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeResourceRate;
import static java.lang.Math.max;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class ResourcesAboveRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "resourcesAbove";
    private static final double DEFAULT_FREE_RESOURCE_RATE_THRESHOLD = 0.4;

    public ResourcesAboveRule(int order, int limit) {
        super(NAME, order, limit);
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isAboveThreshold(clusterInfo)) {
            return max(getLimit(), clusterInfo.getActiveNodes() - 1);
        }
        return 0;
    }

    private boolean isAboveThreshold(ClusterMetricsInfo metrics) {
        return computeFreeResourceRate(metrics) > DEFAULT_FREE_RESOURCE_RATE_THRESHOLD;
    }

}
