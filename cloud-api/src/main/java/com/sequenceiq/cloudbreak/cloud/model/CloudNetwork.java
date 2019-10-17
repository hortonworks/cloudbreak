package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CloudNetwork {

    private String name;

    private String id;

    private Set<CloudSubnet> subnets;

    private Map<String, Object> properties;

    public CloudNetwork(String name, String id, Set<CloudSubnet> subnets, Map<String, Object> properties) {
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
        return subnets.stream().collect(Collectors.toMap(s -> s.getId(), s -> s.getName()));
    }

    public Map<String, CloudSubnet> getSubnetsWithMetadata() {
        return subnets.stream().collect(Collectors.toMap(CloudSubnet::getId, Function.identity()));
    }

    public Set<CloudSubnet> getSubnetsMeta() {
        return subnets;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        CloudNetwork that = (CloudNetwork) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(id, that.id) &&
                Objects.equals(subnets, that.subnets) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, subnets, properties);
    }

    @Override
    public String toString() {
        return "CloudNetwork{"
                + "name='" + name + '\''
                + ", id='" + id + '\''
                + ", subnets=" + subnets
                + ", properties=" + properties
                + '}';
    }
}
