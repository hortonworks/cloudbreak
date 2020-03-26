package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworkV4Response implements JsonEntity {

    private String name;

    private String id;

    private Map<String, String> subnets;

    private Map<String, Object> properties;

    public PlatformNetworkV4Response() {
    }

    public PlatformNetworkV4Response(String name, String id, Map<String, String> subnets, Map<String, Object> properties) {
        this.name = name;
        this.id = id;
        this.subnets = subnets;
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

    public Map<String, String> getSubnets() {
        return subnets;
    }

    public void setSubnets(Map<String, String> subnets) {
        this.subnets = subnets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
