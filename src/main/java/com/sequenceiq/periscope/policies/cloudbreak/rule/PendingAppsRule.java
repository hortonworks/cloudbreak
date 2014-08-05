package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static java.lang.Math.max;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class PendingAppsRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "pendingApps";
    private int pendingAppsLimit;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setLimit(Integer.valueOf(config.get("limit")));
        this.pendingAppsLimit = Integer.valueOf(config.get("pendingApps"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isPendingAppsExceed(clusterInfo)) {
            return max(getLimit(), clusterInfo.getActiveNodes() + 1);
        }
        return 0;
    }

    private boolean isPendingAppsExceed(ClusterMetricsInfo metrics) {
        return metrics.getAppsPending() > pendingAppsLimit;
    }

}