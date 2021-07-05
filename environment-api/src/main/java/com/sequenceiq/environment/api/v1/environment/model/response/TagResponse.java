package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.common.api.tag.response.TaggedResponse;

public class TagResponse implements TaggedResponse, Serializable {

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

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(userDefined.get(key))
                .or(() -> Optional.ofNullable(defaults.get(key)))
                .orElse(null);
    }

    @Override
    public String toString() {
        return "TagResponse{" +
                "userDefined=" + userDefined +
                ", defaults=" + defaults +
                '}';
    }
}
