package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static com.sequenceiq.periscope.policies.cloudbreak.ClusterUtils.computeFreeResourceRate;
import static java.lang.Math.min;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class ResourcesBelowRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "resourcesBelow";
    private static final double DEFAULT_FREE_RESOURCE_RATE_THRESHOLD = 0.2;

    public ResourcesBelowRule(int limit) {
        super(NAME, limit);
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isBelowThreshold(clusterInfo)) {
            return min(getLimit(), clusterInfo.getActiveNodes() + 1);
        }
        return 0;
    }

    private boolean isBelowThreshold(ClusterMetricsInfo metrics) {
        return computeFreeResourceRate(metrics) < DEFAULT_FREE_RESOURCE_RATE_THRESHOLD;
    }

}
