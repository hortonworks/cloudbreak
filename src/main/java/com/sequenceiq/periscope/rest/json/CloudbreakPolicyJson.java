package com.sequenceiq.periscope.rest.json;

import static com.sequenceiq.periscope.utils.CloneUtils.copy;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CloudbreakPolicyJson implements Json {

    private String message;
    private URL jarUrl;
    private Map<String, Map<String, String>> scaleUpRules;
    private Map<String, Map<String, String>> scaleDownRules;

    public CloudbreakPolicyJson() {
    }

    public CloudbreakPolicyJson(Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules) {
        this(scaleUpRules, scaleDownRules, null);
    }

    public CloudbreakPolicyJson(Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules, URL url) {
        this("", scaleUpRules, scaleDownRules, url);
    }

    public CloudbreakPolicyJson(String message,
            Map<String, Map<String, String>> scaleUpRules, Map<String, Map<String, String>> scaleDownRules, URL url) {
        this.message = message;
        this.scaleUpRules = scaleUpRules;
        this.scaleDownRules = scaleDownRules;
        this.jarUrl = url;
    }

    public URL getJarUrl() {
        return jarUrl;
    }

    public void setJarUrl(URL jarUrl) {
        this.jarUrl = jarUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public CloudbreakPolicyJson withMessage(String message) {
        this.message = message;
        return this;
    }

}
