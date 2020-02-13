package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.HashMap;
import java.util.Map;

public class TagResponse {

    private Map<String, String> userDefined = new HashMap<>();

    private Map<String, String> defaults = new HashMap<>();

    public Map<String, String> getUserDefined() {
        return userDefined;
    }

    public void setUserDefined(Map<String, String> userDefined) {
        this.userDefined = userDefined;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }
}
