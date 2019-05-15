package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class CloudGatewayV1Request implements Serializable {
    private String name;

    private String id;

    private Map<String, Object> properties = new HashMap<>();

    public CloudGatewayV1Request() {

    }

    public CloudGatewayV1Request(String name, String id, Map<String, Object> properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
