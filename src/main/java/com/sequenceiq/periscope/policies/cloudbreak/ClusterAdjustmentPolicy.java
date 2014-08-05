package com.sequenceiq.periscope.policies.cloudbreak;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.periscope.policies.cloudbreak.rule.ClusterAdjustmentRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.PendingAppsRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.PendingContainersRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesAboveRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesBelowRule;

@Component
public class ClusterAdjustmentPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterAdjustmentPolicy.class);
    private static final String CLOUDBREAK = "cloudbreak";
    private static final Map<String, Class<? extends ClusterAdjustmentRule>> DEFAULT_RULES;
    private final List<ClusterAdjustmentRule> scaleUpRules = new LinkedList<>();
    private final List<ClusterAdjustmentRule> scaleDownRules = new LinkedList<>();

    static {
        Map<String, Class<? extends ClusterAdjustmentRule>> rules = new TreeMap<>();
        rules.put(ResourcesBelowRule.NAME, ResourcesBelowRule.class);
        rules.put(ResourcesAboveRule.NAME, ResourcesAboveRule.class);
        rules.put(PendingAppsRule.NAME, PendingAppsRule.class);
        rules.put(PendingContainersRule.NAME, PendingContainersRule.class);
        DEFAULT_RULES = Collections.unmodifiableMap(rules);
    }

    @Autowired
    public ClusterAdjustmentPolicy(Yaml yaml, Resource config) throws IOException {
        Map<String, Object> policy = (Map<String, Object>) yaml.loadAs(config.getInputStream(), Properties.class).get(CLOUDBREAK);
        initScaleUpRules(policy);
        initScaleDownRules(policy);
    }

    private void initScaleUpRules(Map<String, Object> policy) {
        initRules((Map<String, Object>) policy.get("scaleUp"), scaleUpRules);
    }

    private void initScaleDownRules(Map<String, Object> policy) {
        initRules((Map<String, Object>) policy.get("scaleDown"), scaleDownRules);
    }

    private void initRules(Map<String, Object> rules, List<ClusterAdjustmentRule> ruleSet) {
        if (rules != null) {
            for (Object rule : rules.keySet()) {
                try {
                    Class<? extends ClusterAdjustmentRule> ruleClass = DEFAULT_RULES.get(rule);
                    if (ruleClass == null) {
                        ruleClass = (Class<? extends ClusterAdjustmentRule>) Class.forName((String) rule);
                    }
                    ClusterAdjustmentRule adjustmentRule = ruleClass.newInstance();
                    adjustmentRule.init((Map<String, Object>) rules.get(rule));
                    ruleSet.add(adjustmentRule);
                } catch (Exception e) {
                    LOGGER.error("Cannot create rule: " + rule, e);
                }
            }
        }
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
