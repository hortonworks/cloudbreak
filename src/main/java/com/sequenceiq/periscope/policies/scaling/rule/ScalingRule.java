package com.sequenceiq.periscope.policies.scaling.rule;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.NamedRule;

public interface ScalingRule extends NamedRule {

    /**
     * Invoked when creating the rule.
     *
     * @param config config parameters
     */
    void init(Map<String, String> config);

    /**
     * Scale up or down to the desired number of nodes.
     *
     * @param clusterInfo cluster metrics obtained from the cluster
     */
    int scale(ClusterMetricsInfo clusterInfo);

    /**
     * Return the maximum/minimum number of nodes. Depends whether its scaling up or down.
     */
    int getLimit();

    /**
     * Returns the number of instances to scale with.
     */
    int getScalingAdjustment();
}
