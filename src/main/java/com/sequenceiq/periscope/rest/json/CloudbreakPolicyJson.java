package com.sequenceiq.periscope.rest.json;

import java.util.HashMap;
import java.util.Map;

public class CloudbreakPolicyJson implements Json {

    private String message;
    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

    public CloudbreakPolicyJson() {
    }

    public CloudbreakPolicyJson(Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
        this("", scaleUpRules, scaleDownRules);
    }

    public CloudbreakPolicyJson(String message,
            Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
        this.message = message;
        this.scaleUpRules = scaleUpRules;
        this.scaleDownRules = scaleDownRules;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Map<String, String>> getScaleUpRules() {
        return scaleUpRules;
    }

    public void setScaleUpRules(Map<String, Map<String, String>> scaleUpRules) {
        this.scaleUpRules = scaleUpRules;
    }

    public Map<String, Map<String, String>> getScaleDownRules() {
        return scaleDownRules;
    }

    public void setScaleDownRules(Map<String, Map<String, String>> scaleDownRules) {
        this.scaleDownRules = scaleDownRules;
    }

    public static CloudbreakPolicyJson emptyJson() {
        Map<String, Map<String, String>> emptyMap = new HashMap<>();
        return new CloudbreakPolicyJson(emptyMap, emptyMap);
    }

}
