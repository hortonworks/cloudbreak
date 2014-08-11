package com.sequenceiq.periscope.policies.cloudbreak;

import static com.sequenceiq.periscope.utils.CloneUtils.copy;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.yarn.server.resourcemanager.webapp.dao.ClusterMetricsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.periscope.policies.cloudbreak.rule.CloudbreakRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.scaledown.ResourcesAboveRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.scaleup.PendingAppsRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.scaleup.PendingContainersRule;
import com.sequenceiq.periscope.policies.cloudbreak.rule.scaleup.ResourcesBelowRule;

public class CloudbreakPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPolicy.class);
    private static final Map<String, Class<? extends CloudbreakRule>> DEFAULT_RULES;
    private final List<CloudbreakRule> scaleUpRules = new LinkedList<>();
    private final List<CloudbreakRule> scaleDownRules = new LinkedList<>();
    private final Map<String, Map<String, String>> scaleUpConfig;
    private final Map<String, Map<String, String>> scaleDownConfig;
    private final URL jarUrl;

    static {
        Map<String, Class<? extends CloudbreakRule>> rules = new TreeMap<>();
        rules.put(ResourcesBelowRule.NAME, ResourcesBelowRule.class);
        rules.put(ResourcesAboveRule.NAME, ResourcesAboveRule.class);
        rules.put(PendingAppsRule.NAME, PendingAppsRule.class);
        rules.put(PendingContainersRule.NAME, PendingContainersRule.class);
        DEFAULT_RULES = Collections.unmodifiableMap(rules);
    }

    public CloudbreakPolicy(Map<String, Map<String, String>> upConfig, Map<String, Map<String, String>> downConfig, URL url) {
        this.scaleUpConfig = upConfig;
        this.scaleDownConfig = downConfig;
        this.jarUrl = url;
        URLClassLoader classLoader = createClassLoader(url);
        initRules(scaleUpConfig, scaleUpRules, classLoader);
        initRules(scaleDownConfig, scaleDownRules, classLoader);
    }

    public Map<String, Map<String, String>> getScaleUpConfig() {
        return copy(scaleUpConfig);
    }

    public Map<String, Map<String, String>> getScaleDownConfig() {
        return copy(scaleDownConfig);
    }

    public URL getJarUrl() {
        return jarUrl;
    }

    public int scale(ClusterMetricsInfo clusterInfo) {
        int scaleTo = scale(clusterInfo, scaleUpRules);
        return scaleTo == 0 ? scale(clusterInfo, scaleDownRules) : scaleTo;
    }

    private URLClassLoader createClassLoader(URL url) {
        URLClassLoader classLoader = null;
        if (url != null) {
            classLoader = new URLClassLoader(new URL[]{url});
        }
        return classLoader;
    }

    private void initRules(Map<String, Map<String, String>> config, List<CloudbreakRule> ruleSet, URLClassLoader classLoader) {
        Iterator<String> iterator = config.keySet().iterator();
        while (iterator.hasNext()) {
            String rule = iterator.next();
            try {
                Class<? extends CloudbreakRule> ruleClass = loadClass(rule, classLoader);
                if (ruleClass != null) {
                    CloudbreakRule cbRule = ruleClass.newInstance();
                    cbRule.init(config.get(rule));
                    ruleSet.add(cbRule);
                } else {
                    LOGGER.info("Cannot load rule class {}", rule);
                    iterator.remove();
                }
            } catch (Exception e) {
                LOGGER.error("Cannot create rule: " + rule, e);
            }
        }
    }

    private Class<? extends CloudbreakRule> loadClass(String rule, URLClassLoader classLoader) {
        Class<? extends CloudbreakRule> clazz = DEFAULT_RULES.get(rule);
        if (clazz == null) {
            try {
                clazz = (Class<? extends CloudbreakRule>) Class.forName(rule);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Cannot load class from classpath: " + rule, e);
            }
        }
        if (clazz == null && classLoader != null) {
            try {
                clazz = (Class<? extends CloudbreakRule>) classLoader.loadClass(rule);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Cannot load class from URL: " + rule, e);
            }
        }
        return clazz;
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

}
