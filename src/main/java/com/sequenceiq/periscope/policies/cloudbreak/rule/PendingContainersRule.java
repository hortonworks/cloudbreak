package com.sequenceiq.periscope.policies.cloudbreak.rule;

import static java.lang.Math.ceil;
import static java.lang.Math.max;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class PendingContainersRule extends AbstractAdjustmentRule implements ClusterAdjustmentRule {

    public static final String NAME = "pendingContainers";
    private static final double DEFAULT_PENDING_CONTAINERS_THRESHOLD = 20;

    public PendingContainersRule(int order, int limit) {
        super(NAME, order, limit);
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        int pendingContainers = clusterInfo.getPendingContainers();
        if (isPendingContainersExceed(pendingContainers)) {
            int containerPerNode = calcAvgContainerPerNode(clusterInfo);
            return max(getLimit(), (int) ceil((double) pendingContainers / containerPerNode));
        }
        return 0;
    }

    private boolean isPendingContainersExceed(int pendingContainers) {
        return pendingContainers > DEFAULT_PENDING_CONTAINERS_THRESHOLD;
    }

    private int calcAvgContainerPerNode(ClusterMetricsInfo clusterInfo) {
        int allocated = clusterInfo.getContainersAllocated();
        int activeNodes = clusterInfo.getActiveNodes();
        return (int) ceil((double) allocated / activeNodes);
    }

}
