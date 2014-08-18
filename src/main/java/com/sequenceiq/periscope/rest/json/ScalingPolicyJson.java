package com.sequenceiq.periscope.rest.json;

import static com.sequenceiq.periscope.utils.CloneUtils.copy;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalingPolicyJson implements Json {

    @JsonProperty("cooldown")
    private int coolDown;
    @JsonProperty("min-size")
    private int minSize;
    @JsonProperty("max-size")
    private int maxSize;
    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

    public ScalingPolicyJson() {
    }

    public ScalingPolicyJson(int coolDown, int minSize, int maxSize,
            Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
        this.coolDown = coolDown;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.scaleUpRules = scaleUpRules;
        this.scaleDownRules = scaleDownRules;
    }

    public Map<String, Map<String, String>> getScaleUpRules() {
        return copy(scaleUpRules);
    }

    public void setScaleUpRules(Map<String, Map<String, String>> scaleUpRules) {
        this.scaleUpRules = scaleUpRules;
    }

    public Map<String, Map<String, String>> getScaleDownRules() {
        return copy(scaleDownRules);
    }

    public void setScaleDownRules(Map<String, Map<String, String>> scaleDownRules) {
        this.scaleDownRules = scaleDownRules;
    }

    public int getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(int coolDown) {
        this.coolDown = coolDown;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public static ScalingPolicyJson emptyJson() {
        Map<String, Map<String, String>> emptyMap = new HashMap<>();
        return new ScalingPolicyJson(0, 0, 0, emptyMap, emptyMap);
    }

}
