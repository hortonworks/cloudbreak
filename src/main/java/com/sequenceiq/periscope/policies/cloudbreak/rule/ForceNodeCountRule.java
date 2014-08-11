package com.sequenceiq.periscope.policies.cloudbreak.rule;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

public class ForceNodeCountRule extends AbstractCloudbreakRule implements CloudbreakRule {

    public static final String NAME = "forceNodeCount";
    private int nodeCount;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        this.nodeCount = Integer.valueOf(config.get("nodeCount"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        return nodeCount;
    }

}