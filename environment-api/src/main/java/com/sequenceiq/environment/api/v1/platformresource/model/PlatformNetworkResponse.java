package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformNetworkResponse implements Serializable {

    private String name;

    private String id;

    private Map<String, String> subnets;

    private Map<String, CloudSubnet> subnetMetadata;

    private Map<String, Object> properties;

    public PlatformNetworkResponse() {
    }

    public PlatformNetworkResponse(String name, String id, Map<String, String> subnets, Map<String, CloudSubnet> subnetMetadata,
            Map<String, Object> properties) {
        this.name = name;
        this.id = id;
        this.subnets = subnets;
        this.subnetMetadata = subnetMetadata;
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

    public Map<String, CloudSubnet> getSubnetMetadata() {
        return subnetMetadata;
    }

    public void setSubnetMetadata(Map<String, CloudSubnet> subnetMetadata) {
        this.subnetMetadata = subnetMetadata;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "PlatformNetworkResponse{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", subnets=" + subnets +
                ", subnetMetadata=" + subnetMetadata +
                ", properties=" + properties +
                '}';
    }
}
