package com.sequenceiq.periscope.policies.scaling.rule.scaledown;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeClusterResourceRate;
import static java.lang.Math.max;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class ResourcesAboveRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "resourcesAbove";
    private double freeResourceRate;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setLimit(Integer.valueOf(config.get("limit")));
        this.freeResourceRate = Double.valueOf(config.get("freeResourceRate"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isAboveThreshold(clusterInfo)) {
            return max(getLimit(), clusterInfo.getActiveNodes() - 1);
        }
        return 0;
    }

    private boolean isAboveThreshold(ClusterMetricsInfo metrics) {
        return computeFreeClusterResourceRate(metrics) > freeResourceRate;
    }

}
