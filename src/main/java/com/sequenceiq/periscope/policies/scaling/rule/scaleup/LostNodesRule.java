package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class LostNodesRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "lostNodes";
    private int lostNodesLimit;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.lostNodesLimit = Integer.valueOf(config.get("lostNodes"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isLostNodesLimitExceed(clusterInfo)) {
            int scalingAdjustment = getScalingAdjustment();
            int currentNodeCount = getCurrentNodeCount(clusterInfo);
            int desiredNodeCount = currentNodeCount +
                    (scalingAdjustment == 0 ? clusterInfo.getLostNodes() : scalingAdjustment);
            return scaleTo(currentNodeCount, desiredNodeCount);
        }
        return 0;
    }

    private boolean isLostNodesLimitExceed(ClusterMetricsInfo metrics) {
        return metrics.getLostNodes() > lostNodesLimit;
    }
}
