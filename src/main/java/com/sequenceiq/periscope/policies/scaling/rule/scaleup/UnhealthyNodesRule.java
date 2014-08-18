package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import static java.lang.Math.min;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class UnhealthyNodesRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "unhealthyNodes";
    private int unhealthyNodesLimit;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setLimit(config.get(RuleProperties.LIMIT));
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.unhealthyNodesLimit = Integer.valueOf(config.get("unhealthyNodes"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isUnhealthyNodesLimitExceed(clusterInfo)) {
            int scalingAdjustment = getScalingAdjustment();
            return min(getLimit(), getCurrentNodeCount(clusterInfo)
                    + (scalingAdjustment == 0 ? clusterInfo.getUnhealthyNodes() : scalingAdjustment));
        }
        return 0;
    }

    private boolean isUnhealthyNodesLimitExceed(ClusterMetricsInfo metrics) {
        return metrics.getUnhealthyNodes() > unhealthyNodesLimit;
    }
}