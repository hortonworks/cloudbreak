package com.sequenceiq.cloudbreak.controller.json;

public class AmbariAddressJson implements JsonEntity {

    private String ambariAddress;

    public String getAmbariAddress() {
        return ambariAddress;
    }

    public void setAmbariAddress(String ambariAddress) {
        this.ambariAddress = ambariAddress;
    }
}
