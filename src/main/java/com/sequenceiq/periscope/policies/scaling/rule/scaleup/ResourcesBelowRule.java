package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeClusterResourceRate;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class ResourcesBelowRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "resourcesBelow";
    private double freeResourceRate;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.freeResourceRate = Double.valueOf(config.get("freeResourceRate"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isBelowThreshold(clusterInfo)) {
            int currentNodeCount = getCurrentNodeCount(clusterInfo);
            int desiredNodeCount = currentNodeCount + getScalingAdjustment();
            return scaleTo(currentNodeCount, desiredNodeCount);
        }
        return 0;
    }

    private boolean isBelowThreshold(ClusterMetricsInfo metrics) {
        return computeFreeClusterResourceRate(metrics) < freeResourceRate;
    }

}
