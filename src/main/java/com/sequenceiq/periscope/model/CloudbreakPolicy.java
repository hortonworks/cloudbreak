package com.sequenceiq.periscope.model;

import java.util.Map;

import com.sequenceiq.periscope.rest.json.CloudbreakPolicyJson;

public class CloudbreakPolicy {

    private final Map<String, Map<String, String>> scaleUpRules;
    private final Map<String, Map<String, String>> scaleDownRules;

    public CloudbreakPolicy(CloudbreakPolicyJson source) {
        scaleDownRules = source.getScaleDownRules();
        scaleUpRules = source.getScaleUpRules();
    }

    public Map<String, Map<String, String>> getScaleUpRules() {
        return scaleUpRules;
    }

    public Map<String, Map<String, String>> getScaleDownRules() {
        return scaleDownRules;
    }
}
