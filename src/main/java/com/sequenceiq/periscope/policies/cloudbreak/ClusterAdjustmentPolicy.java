package com.sequenceiq.periscope.policies.cloudbreak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.policies.cloudbreak.rule.ClusterAdjustmentRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesAboveRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesBelowRule;

@Component
public class ClusterAdjustmentPolicy {

    private static final Map<String, Class<? extends ClusterAdjustmentRule>> DEFAULT_RULES;
    private final List<ClusterAdjustmentRule> scaleUpRules = new ArrayList<>();
    private final List<ClusterAdjustmentRule> scaleDownRules = new ArrayList<>();

    static {
        Map<String, Class<? extends ClusterAdjustmentRule>> rules = new TreeMap<>();
        rules.put(ResourcesBelowRule.NAME, ResourcesBelowRule.class);
        rules.put(ResourcesAboveRule.NAME, ResourcesAboveRule.class);
        DEFAULT_RULES = Collections.unmodifiableMap(rules);
    }

    public ClusterAdjustmentPolicy() {
        scaleUpRules.add(new ResourcesBelowRule(4));
        scaleDownRules.add(new ResourcesAboveRule(3));
    }

    public int scale(ClusterMetricsInfo clusterInfo) {
        int scaleTo = scale(clusterInfo, scaleUpRules);
        return scaleTo == 0 ? scale(clusterInfo, scaleDownRules) : scaleTo;
    }

    private int scale(ClusterMetricsInfo clusterInfo, List<ClusterAdjustmentRule> rules) {
        int scaleTo = 0;
        for (ClusterAdjustmentRule rule : rules) {
            scaleTo = rule.scale(clusterInfo);
            if (scaleTo != 0) {
                break;
            }
        }
        return scaleTo;
    }

}
