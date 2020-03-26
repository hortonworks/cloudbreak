package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.customcontainer;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomContainerV4Request implements JsonEntity {

    private Map<String, String> definitions = new HashMap<>();

    public Map<String, String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, String> definitions) {
        this.definitions = definitions;
    }
}
