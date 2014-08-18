package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import static java.lang.Math.min;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class PendingAppsRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "pendingApps";
    private int pendingAppsLimit;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setLimit(config.get(RuleProperties.LIMIT));
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.pendingAppsLimit = Integer.valueOf(config.get("pendingApps"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isPendingAppsExceed(clusterInfo)) {
            int currentNodeCount = getCurrentNodeCount(clusterInfo);
            int desiredNodeCount = min(getLimit(), currentNodeCount + getScalingAdjustment());
            return scaleTo(currentNodeCount, desiredNodeCount);
        }
        return 0;
    }

    private boolean isPendingAppsExceed(ClusterMetricsInfo metrics) {
        return metrics.getAppsPending() > pendingAppsLimit;
    }

}