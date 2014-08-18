package com.sequenceiq.periscope.rest.json;

import static com.sequenceiq.periscope.utils.CloneUtils.copy;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScalingPolicyJson implements Json {

    @JsonProperty("cooldown")
    private int coolDown;
    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

    public ScalingPolicyJson() {
    }

    public ScalingPolicyJson(int coolDown,
            Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
        this.coolDown = coolDown;
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

    public static ScalingPolicyJson emptyJson() {
        Map<String, Map<String, String>> emptyMap = new HashMap<>();
        return new ScalingPolicyJson(0, emptyMap, emptyMap);
    }

}
