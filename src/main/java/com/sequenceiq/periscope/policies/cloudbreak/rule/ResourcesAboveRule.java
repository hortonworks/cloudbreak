package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeResourceRate;
import static java.lang.Math.max;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class ResourcesAboveRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "resourcesAbove";
    private double freeResourceRate;

    @Override
    public void init(Map<String, Object> config) {
        setName(NAME);
        setLimit((int) config.get("limit"));
        this.freeResourceRate = (double) config.get("freeResourceRate");
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isAboveThreshold(clusterInfo)) {
            return max(getLimit(), clusterInfo.getActiveNodes() - 1);
        }
        return 0;
    }

    private boolean isAboveThreshold(ClusterMetricsInfo metrics) {
        return computeFreeResourceRate(metrics) > freeResourceRate;
    }

}
