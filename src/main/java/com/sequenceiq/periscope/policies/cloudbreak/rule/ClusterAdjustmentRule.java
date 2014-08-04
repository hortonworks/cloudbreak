package com.sequenceiq.periscope.policies.cloudbreak.rule;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.NamedRule;

public interface ClusterAdjustmentRule extends NamedRule, Comparable<ClusterAdjustmentRule> {

    /**
     * Scale up or down to the required number of nodes.
     *
     * @param clusterInfo
     * @return
     */
    int scale(ClusterMetricsInfo clusterInfo);

    /**
     * Rules can be ordered. The first rule will apply.
     *
     * @return order of the rule. 0 is the first rule.
     */
    int getOrder();

    /**
     * Return the maximum/minimum number of nodes. Depends whether its scaling up or down.
     */
    int getLimit();
}
