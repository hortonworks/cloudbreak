package com.sequenceiq.environment.experience.common;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonExperience {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonExperience.class);

    private String name;

    private String description;

    private String internalEnvironmentEndpoint;

    private String policyPath;

    private String address;

    public CommonExperience(String name, String description, String internalEnvironmentEndpoint, String address, String policyPath) {
        this.name = name;
        this.address = address;
        this.policyPath = policyPath;
        this.description = description;
        this.internalEnvironmentEndpoint = internalEnvironmentEndpoint;
        LOGGER.debug("CommonExperience has been created: {}", toString());
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

    public String getPolicyPath() {
        return policyPath;
    }

    public void setPolicyPath(String policyPath) {
        this.policyPath = policyPath;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", CommonExperience.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .add("policyPath='" + policyPath + "'")
                .add("internalEnvironmentEndpoint='" + internalEnvironmentEndpoint + "'")
                .add("address='" + address + "'")
                .toString();
    }

}
