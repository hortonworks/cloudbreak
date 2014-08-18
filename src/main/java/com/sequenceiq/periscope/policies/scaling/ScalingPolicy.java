package com.sequenceiq.periscope.policies.scaling;

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

import com.sequenceiq.periscope.policies.scaling.rule.ForceNodeCountRule;
import com.sequenceiq.periscope.policies.scaling.rule.ScalingRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaledown.ResourcesAboveRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaleup.LostNodesRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaleup.PendingAppsRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaleup.PendingContainersRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaleup.ResourcesBelowRule;
import com.sequenceiq.periscope.policies.scaling.rule.scaleup.UnhealthyNodesRule;

public class ScalingPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingPolicy.class);
    private static final Map<String, Class<? extends ScalingRule>> DEFAULT_RULES;
    private static final int SEC_IN_MS = 1000;
    private final List<ScalingRule> scaleUpRules = new LinkedList<>();
    private final List<ScalingRule> scaleDownRules = new LinkedList<>();
    private final Map<String, Map<String, String>> scaleUpConfig;
    private final Map<String, Map<String, String>> scaleDownConfig;
    private final int coolDown;
    private final int minSize;
    private final int maxSize;
    private long lastScalingActivityStart;

    static {
        Map<String, Class<? extends ScalingRule>> rules = new TreeMap<>();
        rules.put(ResourcesBelowRule.NAME, ResourcesBelowRule.class);
        rules.put(ResourcesAboveRule.NAME, ResourcesAboveRule.class);
        rules.put(PendingAppsRule.NAME, PendingAppsRule.class);
        rules.put(PendingContainersRule.NAME, PendingContainersRule.class);
        rules.put(ForceNodeCountRule.NAME, ForceNodeCountRule.class);
        rules.put(LostNodesRule.NAME, LostNodesRule.class);
        rules.put(UnhealthyNodesRule.NAME, UnhealthyNodesRule.class);
        DEFAULT_RULES = Collections.unmodifiableMap(rules);
    }

    public ScalingPolicy(int coolDown, int minSize, int maxSize,
            Map<String, Map<String, String>> upConfig, Map<String, Map<String, String>> downConfig) {
        this(coolDown, minSize, maxSize, upConfig, downConfig, null);
    }

    public ScalingPolicy(int coolDown, int minSize, int maxSize,
            Map<String, Map<String, String>> upConfig, Map<String, Map<String, String>> downConfig, URL url) {
        this.coolDown = coolDown;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.scaleUpConfig = upConfig;
        this.scaleDownConfig = downConfig;
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

    public int getCoolDown() {
        return coolDown;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int scale(ClusterMetricsInfo clusterInfo) {
        int scaleTo = 0;
        if (canScale()) {
            scaleTo = scale(clusterInfo, scaleUpRules);
            scaleTo = scaleTo == 0 ? scale(clusterInfo, scaleDownRules) : scaleTo;
            if (scaleTo != 0) {
                if (scaleTo > maxSize) {
                    scaleTo = maxSize;
                } else if (scaleTo < minSize) {
                    scaleTo = minSize;
                }
                lastScalingActivityStart = System.currentTimeMillis();
            }
        }
        return scaleTo;
    }

    private URLClassLoader createClassLoader(URL url) {
        URLClassLoader classLoader = null;
        if (url != null) {
            classLoader = new URLClassLoader(new URL[]{url});
        }
        return classLoader;
    }

    private void initRules(Map<String, Map<String, String>> config, List<ScalingRule> ruleSet, URLClassLoader classLoader) {
        Iterator<String> iterator = config.keySet().iterator();
        while (iterator.hasNext()) {
            String rule = iterator.next();
            try {
                Class<? extends ScalingRule> ruleClass = loadClass(rule, classLoader);
                if (ruleClass != null) {
                    ScalingRule cbRule = ruleClass.newInstance();
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

    private Class<? extends ScalingRule> loadClass(String rule, URLClassLoader classLoader) {
        Class<? extends ScalingRule> clazz = DEFAULT_RULES.get(rule);
        if (clazz == null) {
            try {
                clazz = (Class<? extends ScalingRule>) Class.forName(rule);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Cannot load class from classpath: " + rule, e);
            }
        }
        if (clazz == null && classLoader != null) {
            try {
                clazz = (Class<? extends ScalingRule>) classLoader.loadClass(rule);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Cannot load class from URL: " + rule, e);
            }
        }
        return clazz;
    }

    private int scale(ClusterMetricsInfo clusterInfo, List<ScalingRule> rules) {
        int scaleTo = 0;
        for (ScalingRule rule : rules) {
            scaleTo = rule.scale(clusterInfo);
            if (scaleTo != 0) {
                break;
            }
        }
        return scaleTo;
    }

    private boolean canScale() {
        boolean result = false;
        if (lastScalingActivityStart == 0
                || (System.currentTimeMillis() - lastScalingActivityStart) > (coolDown * SEC_IN_MS)) {
            result = true;
        }
        return result;
    }

}
