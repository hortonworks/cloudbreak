package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import static java.lang.Math.max;

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
        setLimit(config.get(RuleProperties.LIMIT));
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.lostNodesLimit = Integer.valueOf(config.get("lostNodes"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isLostNodesLimitExceed(clusterInfo)) {
            int scalingAdjustment = getScalingAdjustment();
            return max(getLimit(), clusterInfo.getActiveNodes()
                    + scalingAdjustment == 0 ? clusterInfo.getLostNodes() : scalingAdjustment);
        }
        return 0;
    }

    private boolean isLostNodesLimitExceed(ClusterMetricsInfo metrics) {
        return metrics.getLostNodes() > lostNodesLimit;
    }
}
