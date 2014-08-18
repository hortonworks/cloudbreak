package com.sequenceiq.periscope.policies.scaling.rule.scaleup;

import static java.lang.Math.ceil;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.scaling.rule.AbstractScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.RuleProperties;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;

public class PendingContainersRule extends AbstractScalingRule implements ScalingRule {

    public static final String NAME = "pendingContainers";
    private int pendingContainersLimit;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setScalingAdjustment(config.get(RuleProperties.SCALING_ADJUSTMENT));
        this.pendingContainersLimit = Integer.valueOf(config.get("pendingContainers"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        int pendingContainers = clusterInfo.getPendingContainers();
        if (isPendingContainersExceed(pendingContainers)) {
            int containerPerNode = calcAvgContainerPerNode(clusterInfo);
            int adjustment = getScalingAdjustment();
            int currentNodeCount = getCurrentNodeCount(clusterInfo);
            int desiredNodeCount = currentNodeCount
                    + (adjustment == 0 ? (int) ceil((double) pendingContainers / containerPerNode) : adjustment);
            return scaleTo(currentNodeCount, desiredNodeCount);
        }
        return 0;
    }

    private boolean isPendingContainersExceed(int pendingContainers) {
        return pendingContainers > pendingContainersLimit;
    }

    private int calcAvgContainerPerNode(ClusterMetricsInfo clusterInfo) {
        int allocated = clusterInfo.getContainersAllocated();
        int activeNodes = getCurrentNodeCount(clusterInfo);
        return (int) ceil((double) allocated / activeNodes);
    }

}
