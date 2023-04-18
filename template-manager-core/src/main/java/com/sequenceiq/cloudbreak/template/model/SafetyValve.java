package com.sequenceiq.cloudbreak.template.model;

import java.util.List;

public class SafetyValve {
    private String serviceType;

    private String roleType;

    private String name;

    private String rawValue;

    private List<SafetyValveProperty> properties;

    public SafetyValve() {

    }

    public SafetyValve(String serviceType, String roleType, String name, String rawValue) {
        this.serviceType = serviceType;
        this.rawValue = rawValue;
        this.roleType = roleType;
        this.name = name;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public List<SafetyValveProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<SafetyValveProperty> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "SafetyValve{" +
                "serviceType='" + serviceType + '\'' +
                ", roleType='" + roleType + '\'' +
                ", name='" + name + '\'' +
                ", rawValue='" + rawValue + '\'' +
                ", properties=" + properties +
                '}';
    }
}
