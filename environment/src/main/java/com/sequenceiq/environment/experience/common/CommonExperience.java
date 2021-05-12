package com.sequenceiq.environment.experience.common;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.commons.lang3.StringUtils;

public class CommonExperience {

    private String name;

    private String description;

    private String businessName;

    private String internalEnvironmentEndpoint;

    private String baseAddress;

    private String policyEndpoint;

    private String policyPort;

    private String address;

    private String environmentEndpointPort;

    public CommonExperience(String name, String description, String internalEnvironmentEndpoint, String address, String businessName, String policyEndpoint,
            String environmentEndpointPort, String baseAddress, String policyPort) {
        this.name = name;
        this.address = address;
        this.policyPort = policyPort;
        this.description = description;
        this.baseAddress = baseAddress;
        this.businessName = businessName;
        this.policyEndpoint = policyEndpoint;
        this.environmentEndpointPort = environmentEndpointPort;
        this.internalEnvironmentEndpoint = internalEnvironmentEndpoint;
        getLogger(CommonExperience.class).debug(CommonExperience.class.getSimpleName() + " has been created: {}", toString());
    }

    public CommonExperience() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyEndpoint() {
        return policyEndpoint;
    }

    public void setPolicyEndpoint(String policyEndpoint) {
        this.policyEndpoint = policyEndpoint;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public boolean hasResourceDeleteAccess() {
        return StringUtils.isNotEmpty(baseAddress);
    }

    public boolean hasFineGradePolicy() {
        return StringUtils.isNotEmpty(policyEndpoint);
    }

    public String getInternalEnvironmentEndpoint() {
        return internalEnvironmentEndpoint;
    }

    public void setInternalEnvironmentEndpoint(String internalEnvironmentEndpoint) {
        this.internalEnvironmentEndpoint = internalEnvironmentEndpoint;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public String getPolicyPort() {
        return policyPort;
    }

    public void setPolicyPort(String policyPort) {
        this.policyPort = policyPort;
    }

    public String getEnvironmentEndpointPort() {
        return environmentEndpointPort;
    }

    public void setEnvironmentEndpointPort(String environmentEndpointPort) {
        this.environmentEndpointPort = environmentEndpointPort;
    }

    @Override
    public String toString() {
        return "CommonExperience{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", businessName='" + businessName + '\'' +
                ", internalEnvironmentEndpoint='" + internalEnvironmentEndpoint + '\'' +
                ", baseAddress='" + baseAddress + '\'' +
                ", policyEndpoint='" + policyEndpoint + '\'' +
                ", policyPort='" + policyPort + '\'' +
                ", address='" + address + '\'' +
                ", environmentEndpointPort='" + environmentEndpointPort + '\'' +
                '}';
    }

}
