package com.sequenceiq.periscope.policies.scaling.rule.scaledown;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeClusterResourceRate;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class ResourcesAboveRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "resourcesAbove";
    private double freeResourceRate;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.freeResourceRate = Double.valueOf(config.get("freeResourceRate"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isAboveThreshold(clusterInfo)) {
            int currentNodeCount = getCurrentNodeCount(clusterInfo);
            int desiredNodeCount = currentNodeCount - getScalingAdjustment();
            return scaleTo(currentNodeCount, desiredNodeCount);
        }
        return 0;
    }

    private boolean isAboveThreshold(ClusterMetricsInfo metrics) {
        return computeFreeClusterResourceRate(metrics) > freeResourceRate;
    }
}
