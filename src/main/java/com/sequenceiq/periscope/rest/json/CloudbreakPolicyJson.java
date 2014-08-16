package com.sequenceiq.periscope.rest.json;

import static com.sequenceiq.periscope.utils.CloneUtils.copy;

import java.util.HashMap;
import java.util.Map;

public class CloudbreakPolicyJson implements Json {

    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

    public CloudbreakPolicyJson() {
    }

    public CloudbreakPolicyJson(Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
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

    public static CloudbreakPolicyJson emptyJson() {
        Map<String, Map<String, String>> emptyMap = new HashMap<>();
        return new CloudbreakPolicyJson(emptyMap, emptyMap);
    }

}
