package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonRawValue;

public class AzureStackDescription extends StackDescription {
    private List<String> cloudServices = new ArrayList<>();

    private List<String> virtualMachines = new ArrayList<>();

    public AzureStackDescription() {

    }

    @JsonRawValue
    public List<String> getCloudServices() {
        return cloudServices;
    }

    public void setCloudServices(List<String> cloudServices) {
        this.cloudServices = cloudServices;
    }

    @JsonRawValue
    public List<String> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(List<String> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }
}
