package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static java.lang.Math.max;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class PendingAppsRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "pendingApps";
    private static final double DEFAULT_PENDING_APPS_THRESHOLD = 5;

    public PendingAppsRule(int order, int limit) {
        super(NAME, order, limit);
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isPendingAppsExceed(clusterInfo)) {
            return max(getLimit(), clusterInfo.getActiveNodes() + 1);
        }
        return 0;
    }

    private boolean isPendingAppsExceed(ClusterMetricsInfo metrics) {
        return metrics.getAppsPending() > DEFAULT_PENDING_APPS_THRESHOLD;
    }

}