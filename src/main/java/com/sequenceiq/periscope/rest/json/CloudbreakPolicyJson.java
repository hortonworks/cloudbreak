package com.sequenceiq.periscope.rest.json;

import java.util.Map;

public class CloudbreakPolicyJson implements Json {

    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

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

}
