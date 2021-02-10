package com.sequenceiq.environment.experience.common;

import java.util.StringJoiner;

public class CommonExperience {

    private String name;

    private String description;

    private String internalEnvironmentEndpoint;

    private String address;

    public CommonExperience(String name, String description, String internalEnvironmentEndpoint, String address) {
        this.name = name;
        this.description = description;
        this.internalEnvironmentEndpoint = internalEnvironmentEndpoint;
        this.address = address;
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
                .add("internalEnvironmentEndpoint='" + internalEnvironmentEndpoint + "'")
                .add("address='" + address + "'")
                .toString();
    }
}
