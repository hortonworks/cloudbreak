package com.sequenceiq.periscope.policies.cloudbreak.rule.scaleup;

import static com.sequenceiq.periscope.utils.ClusterUtils.computeFreeClusterResourceRate;
import static java.lang.Math.min;

import java.util.Map;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;

import com.sequenceiq.periscope.policies.cloudbreak.rule.AbstractCloudbreakRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.CloudbreakRule;

public class ResourcesBelowRule extends AbstractCloudbreakRule implements CloudbreakRule {

    public static final String NAME = "resourcesBelow";
    private double freeResourceRate;

    @Override
    public void init(Map<String, String> config) {
        setName(NAME);
        setLimit(Integer.valueOf(config.get("limit")));
        this.freeResourceRate = Double.valueOf(config.get("freeResourceRate"));
    }

    @Override
    public int scale(ClusterMetricsInfo clusterInfo) {
        if (isBelowThreshold(clusterInfo)) {
            return min(getLimit(), clusterInfo.getActiveNodes() + 1);
        }
        return 0;
    }

    private boolean isBelowThreshold(ClusterMetricsInfo metrics) {
        return computeFreeClusterResourceRate(metrics) < freeResourceRate;
    }

}
