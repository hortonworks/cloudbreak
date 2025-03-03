package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class IpPoolResponse implements Serializable {
    private String name;

    private String id;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> properties = new HashMap<>();

    public IpPoolResponse() {

    }

    public IpPoolResponse(String name, String id, Map<String, Object> properties) {
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

    @Override
    public String toString() {
        return "IpPoolResponse{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", properties=" + properties +
                '}';
    }
}
