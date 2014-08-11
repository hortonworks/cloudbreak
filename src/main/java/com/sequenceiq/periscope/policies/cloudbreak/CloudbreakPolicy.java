package com.sequenceiq.periscope.policies.cloudbreak;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.policies.cloudbreak.rule.CloudbreakRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.PendingAppsRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.PendingContainersRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesAboveRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.ResourcesBelowRule;

public class CloudbreakPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPolicy.class);
    private static final Map<String, Class<? extends CloudbreakRule>> DEFAULT_RULES;
    private final List<CloudbreakRule> scaleUpRules = new LinkedList<>();
    private final List<CloudbreakRule> scaleDownRules = new LinkedList<>();
    private final Map<String, Map<String, String>> scaleUpConfig;
    private final Map<String, Map<String, String>> scaleDownConfig;

    static {
        Map<String, Class<? extends CloudbreakRule>> rules = new TreeMap<>();
        rules.put(ResourcesBelowRule.NAME, ResourcesBelowRule.class);
        rules.put(ResourcesAboveRule.NAME, ResourcesAboveRule.class);
        rules.put(PendingAppsRule.NAME, PendingAppsRule.class);
        rules.put(PendingContainersRule.NAME, PendingContainersRule.class);
        DEFAULT_RULES = Collections.unmodifiableMap(rules);
    }

    public CloudbreakPolicy(Map<String, Map<String, String>> upConfig, Map<String, Map<String, String>> downConfig) {
        this.scaleUpConfig = upConfig;
        this.scaleDownConfig = downConfig;
        initRules(upConfig, scaleUpRules);
        initRules(downConfig, scaleDownRules);
    }

    private void initRules(Map<String, Map<String, String>> config, List<CloudbreakRule> ruleSet) {
        if (config != null) {
            for (String rule : config.keySet()) {
                try {
                    Class<? extends CloudbreakRule> ruleClass = DEFAULT_RULES.get(rule);
                    if (ruleClass == null) {
                        ruleClass = (Class<? extends CloudbreakRule>) Class.forName(rule);
                    }
                    CloudbreakRule cbRule = ruleClass.newInstance();
                    cbRule.init(config.get(rule));
                    ruleSet.add(cbRule);
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

    private int scale(ClusterMetricsInfo clusterInfo, List<CloudbreakRule> rules) {
        int scaleTo = 0;
        for (CloudbreakRule rule : rules) {
            scaleTo = rule.scale(clusterInfo);
            if (scaleTo != 0) {
                break;
            }
        }
        return scaleTo;
    }

    public Map<String, Map<String, String>> getScaleUpConfig() {
        return scaleUpConfig;
    }

    public Map<String, Map<String, String>> getScaleDownConfig() {
        return scaleDownConfig;
    }
}
